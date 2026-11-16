#!/usr/bin/env bash
set -euo pipefail

# Evidence collection, summaries, and cleanup for QA Android UI test workflows.

usage() {
  echo "Usage: $0 {remove-runtime-secrets|pull-allure-results|combine-retry-state|prepare-deflake-bundle|merge-allure-results|summarize-allure-results|export-qa-release-log|cleanup-devices|cleanup-workspace}" >&2
  exit 2
}

remove_runtime_secrets() {
  # Remove both the repo symlink and the runner-owned backing file so publish
  # and cleanup steps cannot accidentally carry runtime secrets forward.
  rm -f secrets.json || true
  if [[ -n "${SECRETS_JSON_PATH:-}" ]]; then
    rm -f "${SECRETS_JSON_PATH}" || true
  fi
}

pull_allure_results() {
  local out_dir="${OUT_DIR:?OUT_DIR not set}"
  mkdir -p "${out_dir}"

  # Retry-aware runs already persist per-attempt results during execution.
  # Upgrade runs may store phase results one level deeper under this root.
  if find "${out_dir}" -type f -name '*-result.json' -print -quit | grep -q .; then
    echo "Per-attempt Allure results already present under ${out_dir}; skipping fallback pull."
    return
  fi

  if [[ -z "${DEVICE_LIST:-}" ]]; then
    echo "No devices detected (skipping allure pull)"
    return
  fi

  read -ra DEVICES <<< "${DEVICE_LIST}"
  local idx=1
  for serial in "${DEVICES[@]}"; do
    # Keep this fallback pull best-effort only; per-attempt pulls are the
    # primary source of truth once retry mode is enabled.
    echo "Pulling allure-results from device ${idx}/${DEVICE_COUNT}..."
    mkdir -p "${out_dir}/${serial}"
    adb -s "${serial}" pull "/sdcard/googletest/test_outputfiles/allure-results" "${out_dir}/${serial}" >/dev/null 2>&1 || true
    idx=$((idx + 1))
  done
}

combine_retry_state() {
  : "${RETRY_STATE_DIRS:?RETRY_STATE_DIRS not set}"
  : "${COMBINED_RETRY_STATE_DIR:?COMBINED_RETRY_STATE_DIR not set}"

  # Upgrade runs can produce separate retry states for the normal and upgrade
  # phases. Merge them back into one contract for the deflake artifact.
  mkdir -p "${COMBINED_RETRY_STATE_DIR}"
  local first_failed_file="${COMBINED_RETRY_STATE_DIR}/first-attempt-failed.txt"
  local final_failed_file="${COMBINED_RETRY_STATE_DIR}/final-failed.txt"
  : > "${first_failed_file}"
  : > "${final_failed_file}"

  read -ra STATE_DIRS <<< "${RETRY_STATE_DIRS}"
  for state_dir in "${STATE_DIRS[@]}"; do
    [[ -d "${state_dir}" ]] || continue

    if [[ -s "${state_dir}/first-attempt-failed.txt" ]]; then
      cat "${state_dir}/first-attempt-failed.txt" >> "${first_failed_file}"
    fi

    local latest_failed_file=""
    while IFS= read -r candidate; do
      latest_failed_file="${candidate}"
    done < <(find "${state_dir}" -maxdepth 1 -type f -name 'attempt-*-failed.txt' | sort -V)

    if [[ -n "${latest_failed_file}" && -s "${latest_failed_file}" ]]; then
      cat "${latest_failed_file}" >> "${final_failed_file}"
    fi
  done

  sort -u "${first_failed_file}" -o "${first_failed_file}"
  sort -u "${final_failed_file}" -o "${final_failed_file}"

  local first_failed_count
  local final_failed_count
  local passed_on_rerun_count=0
  first_failed_count="$(grep -cve '^[[:space:]]*$' "${first_failed_file}" || true)"
  final_failed_count="$(grep -cve '^[[:space:]]*$' "${final_failed_file}" || true)"
  if (( first_failed_count > final_failed_count )); then
    passed_on_rerun_count=$((first_failed_count - final_failed_count))
  fi

  {
    echo "RETRY_STATE_DIR=${COMBINED_RETRY_STATE_DIR}"
    echo "FIRST_FAILED_TESTS_FILE=${first_failed_file}"
    echo "FINAL_FAILED_TESTS_FILE=${final_failed_file}"
    echo "FIRST_FAILED_TESTS_COUNT=${first_failed_count}"
    echo "FINAL_FAILED_TESTS_COUNT=${final_failed_count}"
    echo "PASSED_ON_RERUN_COUNT=${passed_on_rerun_count}"
  } >> "${GITHUB_ENV}"
}

prepare_deflake_bundle() {
  : "${DEFLAKE_BUNDLE_DIR:?DEFLAKE_BUNDLE_DIR not set}"

  if [[ -z "${FINAL_FAILED_TESTS_FILE:-}" || ! -f "${FINAL_FAILED_TESTS_FILE}" ]]; then
    echo "No retry-state file found; skipping deflake bundle export."
    return
  fi

  python3 scripts/qa/android-ui/prepare_deflake_bundle.py
}

merge_allure_results() {
  # One merged dataset lets the final report reflect the latest outcome per
  # logical test instead of showing each rerun attempt as a separate result.
  python3 scripts/qa/android-ui/merge_allure_results.py
}

summarize_allure_results() {
  : "${MERGED_DIR:?MERGED_DIR not set}"
  : "${GITHUB_OUTPUT:?GITHUB_OUTPUT not set}"

  python3 scripts/qa/android-ui/summarize_allure_results.py
}

export_qa_release_log() {
  : "${RELEASE_LOG_OUTPUT:?RELEASE_LOG_OUTPUT not set}"

  # Evidence only. The Python exporter writes nothing for runs that are not
  # production regressions, and never changes test results.
  python3 scripts/qa/android-ui/export_release_log.py --output "${RELEASE_LOG_OUTPUT}"
}

cleanup_devices() {
  if [[ -n "${DEVICE_LIST:-}" ]]; then
    # Remove app data and instrumentation packages as well as pushed upgrade
    # APKs. The test APK contains the limited device credential set and must not
    # remain extractable from a shared physical phone after the run.
    read -ra DEVICES <<< "${DEVICE_LIST}"
    for serial in "${DEVICES[@]}"; do
      if [[ -n "${TEST_APP_ID:-}" ]]; then
        timeout 10s adb -s "${serial}" uninstall "${TEST_APP_ID}" >/dev/null 2>&1 || true
      fi
      if [[ -n "${APP_ID:-}" ]]; then
        timeout 10s adb -s "${serial}" uninstall "${APP_ID}" >/dev/null 2>&1 || true
      fi
      timeout 10s adb -s "${serial}" shell rm -f /data/local/tmp/Wire.old.apk /data/local/tmp/Wire.new.apk || true
    done
  fi
}

cleanup_workspace() {
  : "${ALLURE_RESULTS_DIR:?ALLURE_RESULTS_DIR not set}"
  : "${ALLURE_RESULTS_MERGED_DIR:?ALLURE_RESULTS_MERGED_DIR not set}"

  # Cleanup must be safe to run after both success and failure because every
  # workflow path reaches this step via if: always().
  rm -f "secrets.json" "${RUNNER_TEMP}/secrets.json" || true
  rm -f "${RUNNER_TEMP}/Wire.apk" "${RUNNER_TEMP}/Wire.old.apk" || true

  if [[ "${SKIP_DEVICE_CLEANUP:-false}" != "true" ]]; then
    cleanup_devices
  fi

  rm -rf "${ALLURE_RESULTS_DIR}" || true
  rm -rf "${ALLURE_RESULTS_MERGED_DIR}" || true
  rm -rf "${RUNNER_TEMP}/deflake-input" || true
  rm -rf "${RUNNER_TEMP}/deflake-input-next" || true

  rm -rf "${RUNNER_TEMP}/instrumentation-logs" || true
  rm -f "${RUNNER_TEMP}/private-report-watch.log" || true
  rm -rf "${RUNNER_TEMP}/retry-state" || true
  git clean -ffdx -e .gradle -e .kotlin
}

case "${1:-}" in
  remove-runtime-secrets)
    remove_runtime_secrets
    ;;
  pull-allure-results)
    pull_allure_results
    ;;
  combine-retry-state)
    combine_retry_state
    ;;
  prepare-deflake-bundle)
    prepare_deflake_bundle
    ;;
  merge-allure-results)
    merge_allure_results
    ;;
  summarize-allure-results)
    summarize_allure_results
    ;;
  export-qa-release-log)
    export_qa_release_log
    ;;
  cleanup-devices)
    cleanup_devices
    ;;
  cleanup-workspace)
    cleanup_workspace
    ;;
  *)
    usage
    ;;
esac
