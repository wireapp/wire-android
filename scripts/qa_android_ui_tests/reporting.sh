#!/usr/bin/env bash
set -euo pipefail

# Reporting and publication utilities for QA Android UI test workflows.

usage() {
  echo "Usage: $0 {remove-runtime-secrets|pull-allure-results|combine-retry-state|prepare-deflake-bundle|merge-allure-results|summarize-allure-results|generate-allure-report|publish-allure-report|cleanup-workspace}" >&2
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

  python3 scripts/qa_android_ui_tests/prepare_deflake_bundle.py
}

merge_allure_results() {
  # One merged dataset lets the final report reflect the latest outcome per
  # logical test instead of showing each rerun attempt as a separate result.
  python3 scripts/qa_android_ui_tests/merge_allure_results.py
}

summarize_allure_results() {
  : "${MERGED_DIR:?MERGED_DIR not set}"
  : "${GITHUB_OUTPUT:?GITHUB_OUTPUT not set}"

  python3 scripts/qa_android_ui_tests/summarize_allure_results.py
}

generate_allure_report() {
  : "${MERGED_DIR:?MERGED_DIR not set}"
  : "${REPORT_DIR:?REPORT_DIR not set}"

  if [[ ! -d "${MERGED_DIR}" ]]; then
    echo "No merged Allure results found"
    mkdir -p "${REPORT_DIR}"
    cat > "${REPORT_DIR}/index.html" <<'HTML'
<!doctype html>
<html>
<head><meta charset="utf-8"><title>Allure Report</title></head>
<body><h1>No Allure results found</h1></body>
</html>
HTML
    return
  fi

  if ! ls "${MERGED_DIR}"/*-result.json >/dev/null 2>&1; then
    echo "No Allure result files found"
    mkdir -p "${REPORT_DIR}"
    cat > "${REPORT_DIR}/index.html" <<'HTML'
<!doctype html>
<html>
<head><meta charset="utf-8"><title>Allure Report</title></head>
<body><h1>No Allure result files found</h1></body>
</html>
HTML
    return
  fi

  local allure_version="2.29.0"
  # Update this checksum when bumping allure_version.
  local allure_sha256="a217155db9670ab36ce7b0569b3fb0530a657c81bd7ce5bc974f0bba2a4d84fb"
  local allure_tgz="${RUNNER_TEMP}/allure-${allure_version}.tgz"
  # Download Allure on demand so the runner image does not need to prebundle
  # one specific version forever.
  curl -fsSL -o "${allure_tgz}" \
    "https://github.com/allure-framework/allure2/releases/download/${allure_version}/allure-${allure_version}.tgz"

  if command -v sha256sum >/dev/null 2>&1; then
    if ! echo "${allure_sha256}  ${allure_tgz}" | sha256sum -c - >/dev/null 2>&1; then
      echo "ERROR: Allure checksum verification failed" >&2
      rm -f "${allure_tgz}"
      return 1
    fi
  else
    local actual_sha256
    actual_sha256="$(shasum -a 256 "${allure_tgz}" | awk '{print $1}')"
    if [[ "${actual_sha256}" != "${allure_sha256}" ]]; then
      echo "ERROR: Allure checksum verification failed" >&2
      rm -f "${allure_tgz}"
      return 1
    fi
  fi

  tar -xzf "${allure_tgz}" -C "${RUNNER_TEMP}"
  rm -f "${allure_tgz}"
  "${RUNNER_TEMP}/allure-${allure_version}/bin/allure" \
    generate "${MERGED_DIR}" -o "${REPORT_DIR}" --clean
}

publish_allure_report() {
  : "${REPORT_DIR:?REPORT_DIR not set}"
  : "${PAGES_DIR:?PAGES_DIR not set}"
  : "${KEEP_DAYS:?KEEP_DAYS not set}"
  : "${APK_VERSION:=}"
  : "${APK_NAME:=}"
  : "${PAGES_TITLE:=QA Android UI Tests}"
  : "${GITHUB_RUN_NUMBER:?GITHUB_RUN_NUMBER not set}"
  : "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY not set}"
  : "${GITHUB_STEP_SUMMARY:?GITHUB_STEP_SUMMARY not set}"

  if [[ ! -d "${REPORT_DIR}" ]]; then
    echo "Allure report not found, skipping publish."
    if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
      echo "allure_report_url=" >> "$GITHUB_OUTPUT"
    fi
    return
  fi

  local run_date
  run_date="$(date -u +%Y-%m-%d)"
  local apk_label="${APK_NAME:-${APK_VERSION:-}}"
  local safe_apk
  safe_apk="$(printf '%s' "${apk_label}" | tr -c 'A-Za-z0-9._-' '_' )"
  local run_folder="${run_date}_run-${GITHUB_RUN_NUMBER}"
  if [[ -n "${safe_apk}" ]]; then
    run_folder="${run_folder}_apk-${safe_apk}"
  fi
  local pages_git_path="${PAGES_DIR#gh-pages/}"
  local pages_site_subdir="${pages_git_path#docs/}"

  # Publish each run to its own dated folder so report URLs stay stable and the
  # index page can keep a simple chronological history.
  rm -rf "${PAGES_DIR}/${run_folder}"
  mkdir -p "${PAGES_DIR}/${run_folder}"
  cp -a "${REPORT_DIR}/." "${PAGES_DIR}/${run_folder}/"

  if [[ -n "${KEEP_DAYS}" ]]; then
    # Retention cleanup is date-folder based; deleting old folders keeps Pages
    # size under control without touching newer report snapshots.
    local cutoff
    cutoff="$(date -u -d "${KEEP_DAYS} days ago" +%s)"
    for run_dir in "${PAGES_DIR}"/20??-??-??_run-*; do
      [[ -d "${run_dir}" ]] || continue
      local base
      base="$(basename "${run_dir}")"
      local folder_date="${base%%_*}"
      local ts
      ts="$(date -u -d "${folder_date}" +%s 2>/dev/null || true)"
      if [[ "${ts}" =~ ^[0-9]+$ ]] && (( ts < cutoff )); then
        rm -rf "${run_dir}"
      fi
    done
  fi

  local index_file="${PAGES_DIR}/index.html"
  {
    echo "<!doctype html><html><head><meta charset=\"utf-8\"><title>${PAGES_TITLE}</title></head><body>"
    echo "<h1>${PAGES_TITLE}</h1>"
    echo '<ul>'
    shopt -s nullglob
    runs=( "${PAGES_DIR}"/20??-??-??_run-* )
    shopt -u nullglob
    if [[ ${#runs[@]} -gt 0 ]]; then
      printf '%s\n' "${runs[@]}" | sort -r | while IFS= read -r run_dir; do
        local base
        base="$(basename "${run_dir}")"
        local label="${base//_/ }"
        echo "<li><a href=\"${base}/\">${label}</a></li>"
      done
    fi
    echo '</ul></body></html>'
  } > "${index_file}"

  cd gh-pages
  if [[ -n "$(git status --porcelain)" ]]; then
    git config user.name "github-actions[bot]"
    git config user.email "github-actions[bot]@users.noreply.github.com"
    git add "${pages_git_path}"
    git commit -m "Update Allure report (run ${GITHUB_RUN_NUMBER})"
    git push origin gh-pages
  else
    echo "No changes to publish."
  fi

  local org="${GITHUB_REPOSITORY%%/*}"
  local repo="${GITHUB_REPOSITORY##*/}"
  local base_url="https://${org}.github.io/${repo}"
  local report_url="${base_url}/${pages_site_subdir}/${run_folder}/"
  echo "Allure report (run ${GITHUB_RUN_NUMBER}): ${report_url}" >> "$GITHUB_STEP_SUMMARY"
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "allure_report_url=${report_url}" >> "$GITHUB_OUTPUT"
  fi
}

cleanup_workspace() {
  : "${ALLURE_RESULTS_DIR:?ALLURE_RESULTS_DIR not set}"
  : "${ALLURE_RESULTS_MERGED_DIR:?ALLURE_RESULTS_MERGED_DIR not set}"
  : "${ALLURE_REPORT_DIR:?ALLURE_REPORT_DIR not set}"

  # Cleanup must be safe to run after both success and failure because every
  # workflow path reaches this step via if: always().
  rm -f "secrets.json" "${RUNNER_TEMP}/secrets.json" || true
  rm -f "${RUNNER_TEMP}/Wire.apk" "${RUNNER_TEMP}/Wire.old.apk" || true

  if [[ -n "${DEVICE_LIST:-}" ]]; then
    # Remove APK copies pushed for upgrade runs; each new job pushes fresh files.
    read -ra DEVICES <<< "${DEVICE_LIST}"
    for serial in "${DEVICES[@]}"; do
      adb -s "${serial}" shell rm -f /data/local/tmp/Wire.old.apk /data/local/tmp/Wire.new.apk || true
    done
  fi

  rm -rf "${ALLURE_RESULTS_DIR}" || true
  rm -rf "${ALLURE_RESULTS_MERGED_DIR}" || true
  rm -rf "${ALLURE_REPORT_DIR}" || true
  rm -rf "${RUNNER_TEMP}/deflake-input" || true
  rm -rf "${RUNNER_TEMP}/deflake-input-next" || true

  rm -rf "${RUNNER_TEMP}/instrumentation-logs" || true
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
  generate-allure-report)
    generate_allure_report
    ;;
  publish-allure-report)
    publish_allure_report
    ;;
  cleanup-workspace)
    cleanup_workspace
    ;;
  *)
    usage
    ;;
esac
