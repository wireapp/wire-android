#!/usr/bin/env bash
set -euo pipefail

# Run adb instrumentation on selected devices and aggregate pass/fail status.
: "${DEVICE_LIST:?DEVICE_LIST missing}"
: "${DEVICE_COUNT:?DEVICE_COUNT missing}"
: "${APP_ID:?APP_ID missing}"
: "${TEST_APK_PATH:?TEST_APK_PATH missing}"
: "${RUNNER_TEMP:?RUNNER_TEMP not set}"
: "${TEST_SERVICES_APK_PATH:?TEST_SERVICES_APK_PATH missing}"

# Use one shard per device, but force one shard in single-testcase mode.
NUM_SHARDS="${DEVICE_COUNT}"
if [[ -n "${RESOLVED_TESTCASE_ID:-}" ]]; then
  NUM_SHARDS="1"
fi

read -ra DEVICES <<< "${DEVICE_LIST}"
echo "Sharding: numShards=${NUM_SHARDS}, deviceCount=${DEVICE_COUNT}"

LOG_DIR="${RUNNER_TEMP}/instrumentation-logs"
mkdir -p "${LOG_DIR}"

pids=()
shard_index=0

# Start one background worker per device for parallel execution.
for SERIAL in "${DEVICES[@]}"; do
  (
    set -euo pipefail
    ADB="adb -s ${SERIAL}"

    ${ADB} wait-for-device
    ${ADB} install -r -t "${TEST_APK_PATH}" >/dev/null

    pkgs="$(${ADB} shell pm list packages 2>/dev/null | tr -d '\r' || true)"
    if ! grep -Fxq "package:androidx.test.services" <<< "${pkgs}"; then
      echo "[${SERIAL}] Installing androidx.test.services APK (required for Allure TestStorage)..."
      ${ADB} install -r -t "${TEST_SERVICES_APK_PATH}" >/dev/null
    fi

    if [[ -n "${ORCHESTRATOR_APK_PATH:-}" ]]; then
      pkgs2="$(${ADB} shell pm list packages 2>/dev/null | tr -d '\r' || true)"
      if ! grep -Fxq "package:androidx.test.orchestrator" <<< "${pkgs2}"; then
        echo "[${SERIAL}] Installing androidx.test.orchestrator APK (optional)..."
        ${ADB} install -r -t "${ORCHESTRATOR_APK_PATH}" >/dev/null || true
      fi
    fi

    # Resolve the instrumentation runner by preferred custom runner, then by APP_ID target.
    instr_list="$(${ADB} shell pm list instrumentation 2>/dev/null | tr -d '\r' || true)"
    INSTRUMENTATION="$(printf '%s\n' "${instr_list}" | grep -m1 'TaggedTestRunner' | sed -E 's/^instrumentation:([^ ]+).*/\1/' || true)"
    if [[ -z "${INSTRUMENTATION}" ]]; then
      INSTRUMENTATION="$(printf '%s\n' "${instr_list}" | grep -m1 "target=${APP_ID}" | sed -E 's/^instrumentation:([^ ]+).*/\1/' || true)"
    fi
    if [[ -z "${INSTRUMENTATION}" ]]; then
      echo "[${SERIAL}] ERROR: Could not resolve instrumentation. Installed instrumentations:"
      printf '%s\n' "${instr_list}" | sed -u "s/^/[${SERIAL}] /"
      exit 1
    fi

    THIS_SHARD_INDEX="${shard_index}"
    if [[ "${NUM_SHARDS}" == "1" ]]; then
      THIS_SHARD_INDEX="0"
    fi

    echo "[${SERIAL}] shardIndex=${THIS_SHARD_INDEX}/${NUM_SHARDS}"

    ALLURE_DEVICE_DIR="/sdcard/googletest/test_outputfiles/allure-results"
    ${ADB} shell "rm -rf '${ALLURE_DEVICE_DIR}' && mkdir -p '${ALLURE_DEVICE_DIR}'" >/dev/null 2>&1 || true

    args=()
    args+=(-e numShards "${NUM_SHARDS}")
    args+=(-e shardIndex "${THIS_SHARD_INDEX}")

    if [[ -n "${RESOLVED_TESTCASE_ID:-}" ]]; then
      args+=(-e testCaseId "${RESOLVED_TESTCASE_ID}")
    fi
    if [[ -n "${RESOLVED_CATEGORY:-}" ]]; then
      args+=(-e category "${RESOLVED_CATEGORY}")
    fi

    args+=(-e filter "com.wire.android.tests.support.suite.TaggedFilter")

    if [[ "${IS_UPGRADE:-}" == "true" ]]; then
      args+=(-e newApkPath "${NEW_APK_DEVICE_PATH}")
      args+=(-e oldApkPath "${OLD_APK_DEVICE_PATH}")
    fi

    LOG_FILE="${LOG_DIR}/instrument-${SERIAL}.log"

    set +e
    ${ADB} shell am instrument -w -r "${args[@]}" "${INSTRUMENTATION}" 2>&1 \
      | sed -u "s/^/[${SERIAL}] /" | tee "${LOG_FILE}"
    rc=${PIPESTATUS[0]}
    set -e

    if [[ "${rc}" -ne 0 ]]; then
      echo "[${SERIAL}] instrumentation command failed (rc=${rc})"
      exit 1
    fi

    # Treat known failure markers as failures even when instrumentation exits 0.
    if grep -qE 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_RESULT: shortMsg=Process crashed|INSTRUMENTATION_STATUS_CODE: -1|INSTRUMENTATION_CODE: -1' "${LOG_FILE}"; then
      echo "[${SERIAL}] FAIL"
      exit 1
    fi

    echo "[${SERIAL}] PASS"
    exit 0
  ) &
  pids+=("$!")

  shard_index=$((shard_index + 1))
done

failed=0
# Wait for all workers and fail the step if any shard fails.
for pid in "${pids[@]}"; do
  if ! wait "$pid"; then
    failed=1
  fi
done

if [[ "$failed" -ne 0 ]]; then
  echo "ERROR: One or more shards failed."
  exit 1
fi
