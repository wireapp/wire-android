#!/usr/bin/env bash
set -euo pipefail

# Reporting and publication utilities for qa-android-ui-tests workflow.

usage() {
  echo "Usage: $0 {remove-runtime-secrets|pull-allure-results|merge-allure-results|generate-allure-report|publish-allure-report|cleanup-workspace}" >&2
  exit 2
}

remove_runtime_secrets() {
  rm -f secrets.json || true
  if [[ -n "${SECRETS_JSON_PATH:-}" ]]; then
    rm -f "${SECRETS_JSON_PATH}" || true
  fi
}

pull_allure_results() {
  if [[ -z "${DEVICE_LIST:-}" ]]; then
    echo "No devices detected (skipping allure pull)"
    return
  fi

  local out_dir="${OUT_DIR:?OUT_DIR not set}"
  mkdir -p "${out_dir}"

  read -ra DEVICES <<< "${DEVICE_LIST}"
  local idx=1
  for serial in "${DEVICES[@]}"; do
    echo "Pulling allure-results from device ${idx}/${DEVICE_COUNT}..."
    mkdir -p "${out_dir}/${serial}"
    adb -s "${serial}" pull "/sdcard/googletest/test_outputfiles/allure-results" "${out_dir}/${serial}" >/dev/null 2>&1 || true
    idx=$((idx + 1))
  done
}

merge_allure_results() {
  python3 scripts/qa_android_ui_tests/merge_allure_results.py
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
  local allure_tgz="${RUNNER_TEMP}/allure-${allure_version}.tgz"
  curl -fsSL -o "${allure_tgz}" \
    "https://github.com/allure-framework/allure2/releases/download/${allure_version}/allure-${allure_version}.tgz"
  tar -xzf "${allure_tgz}" -C "${RUNNER_TEMP}"
  "${RUNNER_TEMP}/allure-${allure_version}/bin/allure" \
    generate "${MERGED_DIR}" -o "${REPORT_DIR}" --clean
}

publish_allure_report() {
  : "${REPORT_DIR:?REPORT_DIR not set}"
  : "${PAGES_DIR:?PAGES_DIR not set}"
  : "${KEEP_DAYS:?KEEP_DAYS not set}"
  : "${APK_VERSION:=}"
  : "${APK_NAME:=}"
  : "${GITHUB_RUN_NUMBER:?GITHUB_RUN_NUMBER not set}"
  : "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY not set}"
  : "${GITHUB_STEP_SUMMARY:?GITHUB_STEP_SUMMARY not set}"

  if [[ ! -d "${REPORT_DIR}" ]]; then
    echo "Allure report not found, skipping publish."
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

  rm -rf "${PAGES_DIR}/${run_folder}"
  mkdir -p "${PAGES_DIR}/${run_folder}"
  cp -a "${REPORT_DIR}/." "${PAGES_DIR}/${run_folder}/"

  if [[ -n "${KEEP_DAYS}" ]]; then
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
    echo '<!doctype html><html><head><meta charset="utf-8"><title>QA Android UI Tests</title></head><body>'
    echo '<h1>QA Android UI Tests</h1>'
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
    git add docs/qa-ui-tests
    git commit -m "Update Allure report (run ${GITHUB_RUN_NUMBER})"
    git push origin gh-pages
  else
    echo "No changes to publish."
  fi

  local org="${GITHUB_REPOSITORY%%/*}"
  local repo="${GITHUB_REPOSITORY##*/}"
  local base_url="https://${org}.github.io/${repo}"
  echo "Allure report (run ${GITHUB_RUN_NUMBER}): ${base_url}/qa-ui-tests/${run_folder}/" >> "$GITHUB_STEP_SUMMARY"
}

cleanup_workspace() {
  : "${ALLURE_RESULTS_DIR:?ALLURE_RESULTS_DIR not set}"
  : "${ALLURE_RESULTS_MERGED_DIR:?ALLURE_RESULTS_MERGED_DIR not set}"
  : "${ALLURE_REPORT_DIR:?ALLURE_REPORT_DIR not set}"

  rm -f "secrets.json" "${RUNNER_TEMP}/secrets.json" || true
  rm -f "${RUNNER_TEMP}/Wire.apk" "${RUNNER_TEMP}/Wire.old.apk" || true

  rm -rf "${ALLURE_RESULTS_DIR}" || true
  rm -rf "${ALLURE_RESULTS_MERGED_DIR}" || true
  rm -rf "${ALLURE_REPORT_DIR}" || true

  rm -rf "${RUNNER_TEMP}/instrumentation-logs" || true
  git clean -ffdx -e .gradle -e .kotlin
}

case "${1:-}" in
  remove-runtime-secrets)
    remove_runtime_secrets
    ;;
  pull-allure-results)
    pull_allure_results
    ;;
  merge-allure-results)
    merge_allure_results
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
