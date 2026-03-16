#!/usr/bin/env bash
set -euo pipefail

# Run attempt 0 and retry only failed tests based on workflow rerun inputs.
: "${DEVICE_LIST:?DEVICE_LIST missing}"
: "${DEVICE_COUNT:?DEVICE_COUNT missing}"
: "${APP_ID:?APP_ID missing}"
: "${TEST_APK_PATH:?TEST_APK_PATH missing}"
: "${RUNNER_TEMP:?RUNNER_TEMP not set}"
: "${TEST_SERVICES_APK_PATH:?TEST_SERVICES_APK_PATH missing}"

is_true() {
  [[ "${1:-}" == "true" ]]
}

RERUN_FAILED_ENABLED="${RERUN_FAILED_ENABLED:-true}"
RERUN_FAILED_COUNT="${RERUN_FAILED_COUNT:-1}"
ALLURE_RESULTS_ROOT="${ALLURE_RESULTS_ROOT:-${RUNNER_TEMP}/allure-results}"
ALLURE_PULL_MAX_ATTEMPTS="${ALLURE_PULL_MAX_ATTEMPTS:-3}"
ALLURE_PULL_BASE_DELAY_SEC="${ALLURE_PULL_BASE_DELAY_SEC:-5}"
RERUN_INLINE_PART_MAX_CHARS="${RERUN_INLINE_PART_MAX_CHARS:-7000}"

if [[ ! "${RERUN_FAILED_ENABLED}" =~ ^(true|false)$ ]]; then
  echo "ERROR: RERUN_FAILED_ENABLED must be true or false."
  exit 1
fi

if [[ ! "${RERUN_FAILED_COUNT}" =~ ^[0-9]+$ ]]; then
  echo "ERROR: RERUN_FAILED_COUNT must be a whole number >= 0."
  exit 1
fi

if [[ ! "${ALLURE_PULL_MAX_ATTEMPTS}" =~ ^[0-9]+$ || "${ALLURE_PULL_MAX_ATTEMPTS}" == "0" ]]; then
  echo "ERROR: ALLURE_PULL_MAX_ATTEMPTS must be a whole number >= 1."
  exit 1
fi

if [[ ! "${ALLURE_PULL_BASE_DELAY_SEC}" =~ ^[0-9]+$ ]]; then
  echo "ERROR: ALLURE_PULL_BASE_DELAY_SEC must be a whole number >= 0."
  exit 1
fi

if [[ ! "${RERUN_INLINE_PART_MAX_CHARS}" =~ ^[0-9]+$ || $((10#${RERUN_INLINE_PART_MAX_CHARS})) -lt 256 ]]; then
  echo "ERROR: RERUN_INLINE_PART_MAX_CHARS must be a whole number >= 256."
  exit 1
fi

MAX_RERUNS=0
if is_true "${RERUN_FAILED_ENABLED}"; then
  MAX_RERUNS="$((10#${RERUN_FAILED_COUNT}))"
fi
MAX_PULL_ATTEMPTS="$((10#${ALLURE_PULL_MAX_ATTEMPTS}))"
PULL_BASE_DELAY_SEC="$((10#${ALLURE_PULL_BASE_DELAY_SEC}))"
INLINE_PART_MAX_CHARS="$((10#${RERUN_INLINE_PART_MAX_CHARS}))"

LOG_DIR="${RUNNER_TEMP}/instrumentation-logs"
STATE_DIR="${RUNNER_TEMP}/retry-state"
mkdir -p "${LOG_DIR}" "${STATE_DIR}" "${ALLURE_RESULTS_ROOT}"

read -ra DEVICES <<< "${DEVICE_LIST}"
RETRY_DEVICES=("${DEVICES[@]}")
RETRY_NUM_SHARDS="${#RETRY_DEVICES[@]}"

BASE_NUM_SHARDS="${DEVICE_COUNT}"
if [[ -n "${RESOLVED_TESTCASE_ID:-}" ]]; then
  BASE_NUM_SHARDS="1"
fi

echo "Sharding (attempt 0): numShards=${BASE_NUM_SHARDS}, deviceCount=${DEVICE_COUNT}"
echo "Retry config: enabled=${RERUN_FAILED_ENABLED}, maxReruns=${MAX_RERUNS}, retryDevices=${RETRY_DEVICES[*]}"
echo "Allure pull retries: maxAttempts=${MAX_PULL_ATTEMPTS}, baseDelaySec=${PULL_BASE_DELAY_SEC}"
echo "Retry inline transport: partMaxChars=${INLINE_PART_MAX_CHARS}"

declare -a RERUN_INLINE_PARTS=()

extract_failed_ids() {
  local attempt="$1"
  local failed_output="$2"
  ATTEMPT_RESULTS_DIR="${ALLURE_RESULTS_ROOT}/attempt-${attempt}" \
    FAILED_TESTS_FILE="${failed_output}" \
    python3 scripts/qa_android_ui_tests/extract_failed_tests.py
}

build_rerun_inline_parts() {
  local list_file="$1"
  local max_chars="$2"
  local line=""
  local current=""
  RERUN_INLINE_PARTS=()

  # Split large rerun lists into multiple instrumentation args so we stay below per-arg limits.
  while IFS= read -r line || [[ -n "${line}" ]]; do
    line="${line%$'\r'}"
    [[ -z "${line}" ]] && continue

    if (( ${#line} > max_chars )); then
      echo "ERROR: Retry test ID exceeds part size limit (${#line} > ${max_chars}): ${line}"
      return 1
    fi

    if [[ -z "${current}" ]]; then
      current="${line}"
      continue
    fi

    if (( ${#current} + 1 + ${#line} <= max_chars )); then
      current="${current},${line}"
    else
      RERUN_INLINE_PARTS+=("${current}")
      current="${line}"
    fi
  done < "${list_file}"

  if [[ -n "${current}" ]]; then
    RERUN_INLINE_PARTS+=("${current}")
  fi

  if [[ ${#RERUN_INLINE_PARTS[@]} -eq 0 ]]; then
    echo "ERROR: Computed empty rerun inline parts."
    return 1
  fi
}

device_reported_zero_tests() {
  local attempt="$1"
  local serial="$2"
  local log_file="${LOG_DIR}/attempt-${attempt}-instrument-${serial}.log"
  [[ -f "${log_file}" ]] || return 1

  # Zero-test shards are valid for filtered/sharded runs and should not fail the pull step.
  grep -qE 'INSTRUMENTATION_STATUS: numtests=0|OK \(0 tests\)|No tests found' "${log_file}"
}

pull_allure_results_for_attempt() {
  local attempt="$1"
  shift
  local devices=("$@")

  if [[ ${#devices[@]} -eq 0 ]]; then
    return
  fi

  local attempt_dir="${ALLURE_RESULTS_ROOT}/attempt-${attempt}"
  mkdir -p "${attempt_dir}"

  # Support both pull layouts: <serial>/allure-results/* and <serial>/*.
  has_result_files() {
    local device_dir="$1"
    if compgen -G "${device_dir}/allure-results/*-result.json" >/dev/null; then
      return 0
    fi
    if compgen -G "${device_dir}/*-result.json" >/dev/null; then
      return 0
    fi
    return 1
  }

  for serial in "${devices[@]}"; do
    local device_dir="${attempt_dir}/${serial}"
    local pulled_ok=0
    local pull_try=0

    for ((pull_try = 1; pull_try <= MAX_PULL_ATTEMPTS; pull_try++)); do
      rm -rf "${device_dir}"
      mkdir -p "${device_dir}"

      if adb -s "${serial}" pull "/sdcard/googletest/test_outputfiles/allure-results" "${device_dir}" >/dev/null 2>&1; then
        if has_result_files "${device_dir}"; then
          pulled_ok=1
          break
        fi
        echo "[${serial}] No Allure result files found after pull (attempt ${attempt}, pullTry ${pull_try}/${MAX_PULL_ATTEMPTS})."
      else
        echo "[${serial}] Failed to pull allure-results (attempt ${attempt}, pullTry ${pull_try}/${MAX_PULL_ATTEMPTS})."
      fi

      if (( pull_try < MAX_PULL_ATTEMPTS )); then
        local sleep_seconds=$((PULL_BASE_DELAY_SEC * pull_try))
        if (( sleep_seconds > 0 )); then
          echo "[${serial}] Retrying allure pull in ${sleep_seconds}s..."
          sleep "${sleep_seconds}"
        fi
      fi
    done

    if (( pulled_ok == 0 )); then
      # Some shards legitimately execute zero tests; in that case there is nothing to pull.
      if device_reported_zero_tests "${attempt}" "${serial}"; then
        echo "[${serial}] No Allure result files for attempt ${attempt} because this shard executed zero tests."
        continue
      fi
      echo "ERROR: Failed to pull valid allure-results from ${serial} after ${MAX_PULL_ATTEMPTS} attempt(s)."
      return 1
    fi
  done
}

run_attempt_on_devices() {
  local attempt="$1"
  local num_shards="$2"
  shift 2
  local devices=("$@")
  local failed=0
  local pids=()
  local shard_index=0

  for serial in "${devices[@]}"; do
    (
      set -euo pipefail

      local adb_cmd="adb -s ${serial}"
      ${adb_cmd} wait-for-device
      ${adb_cmd} install -r -t "${TEST_APK_PATH}" >/dev/null

      local pkgs
      pkgs="$(${adb_cmd} shell pm list packages 2>/dev/null | tr -d '\r' || true)"
      if ! grep -Fxq "package:androidx.test.services" <<< "${pkgs}"; then
        echo "[${serial}] Installing androidx.test.services APK (required for Allure TestStorage)..."
        ${adb_cmd} install -r -t "${TEST_SERVICES_APK_PATH}" >/dev/null
      fi

      if [[ -n "${ORCHESTRATOR_APK_PATH:-}" ]]; then
        local pkgs2
        pkgs2="$(${adb_cmd} shell pm list packages 2>/dev/null | tr -d '\r' || true)"
        if ! grep -Fxq "package:androidx.test.orchestrator" <<< "${pkgs2}"; then
          echo "[${serial}] Installing androidx.test.orchestrator APK (optional)..."
          ${adb_cmd} install -r -t "${ORCHESTRATOR_APK_PATH}" >/dev/null || true
        fi
      fi

      local instr_list instrumentation
      instr_list="$(${adb_cmd} shell pm list instrumentation 2>/dev/null | tr -d '\r' || true)"
      instrumentation="$(printf '%s\n' "${instr_list}" | grep -m1 'TaggedTestRunner' | sed -E 's/^instrumentation:([^ ]+).*/\1/' || true)"
      if [[ -z "${instrumentation}" ]]; then
        instrumentation="$(printf '%s\n' "${instr_list}" | grep -m1 "target=${APP_ID}" | sed -E 's/^instrumentation:([^ ]+).*/\1/' || true)"
      fi
      if [[ -z "${instrumentation}" ]]; then
        echo "[${serial}] ERROR: Could not resolve instrumentation. Installed instrumentations:"
        printf '%s\n' "${instr_list}" | sed -u "s/^/[${serial}] /"
        exit 1
      fi

      local this_shard_index="${shard_index}"
      if [[ "${num_shards}" == "1" ]]; then
        this_shard_index="0"
      fi

      echo "[${serial}] attempt=${attempt} shardIndex=${this_shard_index}/${num_shards}"

      local allure_device_dir="/sdcard/googletest/test_outputfiles/allure-results"
      ${adb_cmd} shell "rm -rf '${allure_device_dir}' && mkdir -p '${allure_device_dir}'" >/dev/null 2>&1 || true

      local args=()
      args+=(-e numShards "${num_shards}")
      args+=(-e shardIndex "${this_shard_index}")
      args+=(-e filter "com.wire.android.tests.support.suite.TaggedFilter")

      if (( attempt == 0 )); then
        if [[ -n "${RESOLVED_TESTCASE_ID:-}" ]]; then
          args+=(-e testCaseId "${RESOLVED_TESTCASE_ID}")
        fi
        if [[ -n "${RESOLVED_CATEGORY:-}" ]]; then
          args+=(-e category "${RESOLVED_CATEGORY}")
        fi
      else
        if [[ ${#RERUN_INLINE_PARTS[@]} -eq 0 ]]; then
          echo "[${serial}] ERROR: RERUN_INLINE_PARTS is empty for retry attempt ${attempt}."
          exit 1
        fi
        args+=(-e enableRerunMode "true")
        args+=(-e rerunAttempt "${attempt}")
        args+=(-e rerunListInline "${RERUN_INLINE_PARTS[0]}")
        if (( ${#RERUN_INLINE_PARTS[@]} > 1 )); then
          local part_index=1
          while (( part_index < ${#RERUN_INLINE_PARTS[@]} )); do
            args+=(-e "rerunListInlinePart${part_index}" "${RERUN_INLINE_PARTS[part_index]}")
            part_index=$((part_index + 1))
          done
        fi
      fi

      if [[ "${IS_UPGRADE:-}" == "true" ]]; then
        args+=(-e newApkPath "${NEW_APK_DEVICE_PATH}")
        args+=(-e oldApkPath "${OLD_APK_DEVICE_PATH}")
      fi

      local log_file="${LOG_DIR}/attempt-${attempt}-instrument-${serial}.log"

      set +e
      ${adb_cmd} shell am instrument -w -r "${args[@]}" "${instrumentation}" 2>&1 \
        | sed -u "s/^/[${serial}] /" | tee "${log_file}"
      local rc=${PIPESTATUS[0]}
      set -e

      # Keep normal test failures separate from infra failures so retries can still be resolved from Allure.
      local has_test_failures=0
      if grep -qE 'FAILURES!!!' "${log_file}"; then
        has_test_failures=1
      fi

      if [[ "${rc}" -ne 0 ]]; then
        if (( has_test_failures == 1 )); then
          echo "[${serial}] instrumentation finished with test failures (rc=${rc}); status will be resolved from Allure results."
        else
          echo "[${serial}] instrumentation command failed (rc=${rc})"
          exit 1
        fi
      fi

      if grep -qE 'INSTRUMENTATION_FAILED|INSTRUMENTATION_RESULT: shortMsg=Process crashed|INSTRUMENTATION_RESULT: shortMsg=Process crashed\.' "${log_file}"; then
        echo "[${serial}] INFRA_FAIL"
        exit 1
      fi

      if (( has_test_failures == 1 )); then
        echo "[${serial}] TEST_FAILURE_DETECTED"
      else
        echo "[${serial}] PASS"
      fi
      exit 0
    ) &
    pids+=("$!")

    shard_index=$((shard_index + 1))
  done

  for pid in "${pids[@]}"; do
    if ! wait "${pid}"; then
      failed=1
    fi
  done

  return "${failed}"
}

# Keep the initial failed list in a separate file so attempt 0 bookkeeping
# does not try to copy a file onto itself before reruns begin.
first_failed_file="${STATE_DIR}/first-attempt-failed.txt"
current_failed_file=""
attempt=0
overall_infra_failed=0
declare -a infra_failed_attempts=()

while true; do
  if (( attempt == 0 )); then
    attempt_num_shards="${BASE_NUM_SHARDS}"
    attempt_devices=("${DEVICES[@]}")
  else
    attempt_num_shards="${RETRY_NUM_SHARDS}"
    attempt_devices=("${RETRY_DEVICES[@]}")
  fi

  echo "=== Attempt ${attempt} ==="
  attempt_worker_failed=0
  if ! run_attempt_on_devices "${attempt}" "${attempt_num_shards}" "${attempt_devices[@]}"; then
    attempt_worker_failed=1
    overall_infra_failed=1
    infra_failed_attempts+=("${attempt}")
  fi

  if ! pull_allure_results_for_attempt "${attempt}" "${attempt_devices[@]}"; then
    exit 1
  fi

  attempt_failed_file="${STATE_DIR}/attempt-${attempt}-failed.txt"
  extract_failed_ids "${attempt}" "${attempt_failed_file}"
  failed_count="$(wc -l < "${attempt_failed_file}" | tr -d ' ')"
  echo "Attempt ${attempt} failed tests: ${failed_count}"

  if (( attempt == 0 )); then
    cp "${attempt_failed_file}" "${first_failed_file}"
  fi
  current_failed_file="${attempt_failed_file}"

  if (( failed_count == 0 )); then
    if (( attempt_worker_failed != 0 )); then
      echo "ERROR: Instrumentation failed without detectable failed tests in attempt ${attempt}."
      exit 1
    fi
    break
  fi

  if (( attempt >= MAX_RERUNS )); then
    break
  fi

  next_attempt=$((attempt + 1))
  rerun_list_local="${STATE_DIR}/attempt-${next_attempt}-rerun-list.txt"
  cp "${attempt_failed_file}" "${rerun_list_local}"

  # Limit retry shards to the number of failed tests so the same test is not retried on multiple devices.
  failed_count_num=$((10#${failed_count}))
  retry_device_count="${#DEVICES[@]}"
  if (( failed_count_num < retry_device_count )); then
    retry_device_count="${failed_count_num}"
  fi
  if (( retry_device_count < 1 )); then
    retry_device_count=1
  fi
  RETRY_DEVICES=("${DEVICES[@]:0:${retry_device_count}}")
  RETRY_NUM_SHARDS="${#RETRY_DEVICES[@]}"

  if ! build_rerun_inline_parts "${rerun_list_local}" "${INLINE_PART_MAX_CHARS}"; then
    exit 1
  fi
  echo "Prepared rerun attempt ${next_attempt}: tests=${failed_count}, devices=${RETRY_DEVICES[*]}, numShards=${RETRY_NUM_SHARDS}, inlineParts=${#RERUN_INLINE_PARTS[@]}."

  attempt="${next_attempt}"
done

first_failed_count=0
if [[ -s "${first_failed_file}" ]]; then
  first_failed_count="$(wc -l < "${first_failed_file}" | tr -d ' ')"
fi

final_failed_count=0
if [[ -n "${current_failed_file}" && -s "${current_failed_file}" ]]; then
  final_failed_count="$(wc -l < "${current_failed_file}" | tr -d ' ')"
fi

recovered_count=0
if (( first_failed_count > final_failed_count )); then
  recovered_count=$((first_failed_count - final_failed_count))
fi

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "### UI Test Retry Summary"
    echo "- rerun enabled: ${RERUN_FAILED_ENABLED}"
    echo "- max reruns configured: ${MAX_RERUNS}"
    echo "- failed on first attempt: ${first_failed_count}"
    echo "- passed on rerun: ${recovered_count}"
    echo "- failed after retries: ${final_failed_count}"
    if (( overall_infra_failed > 0 )); then
      echo "- infra shard failures: yes (attempts: ${infra_failed_attempts[*]})"
    else
      echo "- infra shard failures: no"
    fi
  } >> "${GITHUB_STEP_SUMMARY}"
fi

if (( final_failed_count > 0 )); then
  echo "ERROR: ${final_failed_count} test(s) still failing after retries."
  exit 1
fi

if (( overall_infra_failed > 0 )); then
  echo "ERROR: Infrastructure-level shard failures occurred in attempt(s): ${infra_failed_attempts[*]}."
  exit 1
fi
