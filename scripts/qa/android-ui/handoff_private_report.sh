#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?Reporting token is required}"
: "${BOT_LOGIN:?BOT_LOGIN repository variable is required}"
: "${BOT_ID:?BOT_ID repository variable is required}"
: "${REPORTING_REPOSITORY:?Reporting repository is required}"

report_id="${GITHUB_RUN_ID}-${GITHUB_RUN_ATTEMPT}"
# Check destination privacy before preparing and uploading raw test evidence.
ref=$(gh api "repos/${REPORTING_REPOSITORY}" --jq \
  'if .visibility == "internal" or .visibility == "private" then .default_branch else error("Reporting repository must be internal or private") end')
base_url=$(gh api "repos/${REPORTING_REPOSITORY}/pages" --jq \
  'if .public == false and .build_type == "workflow" then .html_url else error("Private Pages with GitHub Actions publishing is required") end')
python3 scripts/qa/android-ui/prepare_private_report.py

git -C private-reports switch --quiet -c "reports/${report_id}"
git -C private-reports add incoming
git -C private-reports -c "user.name=${BOT_LOGIN}" \
  -c "user.email=${BOT_ID}+${BOT_LOGIN}@users.noreply.github.com" \
  commit --quiet -m "ci: collect Android evidence ${report_id}"
git -C private-reports push --quiet origin "reports/${report_id}"

run_id=$(gh api --method POST -H 'X-GitHub-Api-Version: 2022-11-28' \
  "repos/${REPORTING_REPOSITORY}/actions/workflows/publish-android-report.yml/dispatches" \
  -f "ref=${ref}" -f "inputs[report_id]=${report_id}" \
  -F return_run_details=true --jq '.workflow_run_id')
if [[ ! "$run_id" =~ ^[1-9][0-9]*$ ]]; then
  echo "::error::Reporting dispatch did not return a valid workflow_run_id."
  exit 1
fi
private_run="https://github.com/${REPORTING_REPOSITORY}/actions/runs/${run_id}"
echo "Private reporting workflow: ${private_run}" >> "$GITHUB_STEP_SUMMARY"
# Keep receiver diagnostics out of the public Android console.
if ! gh run watch "$run_id" -R "$REPORTING_REPOSITORY" --exit-status \
    > "$RUNNER_TEMP/private-report-watch.log" 2>&1; then
  echo "::error::Private reporting did not succeed. Open the private reporting workflow for details."
  exit 1
fi
report_url="${base_url%/}/runs/${report_id}/"
echo "allure_report_url=${report_url}" >> "$GITHUB_OUTPUT"
echo "Allure report: ${report_url}" >> "$GITHUB_STEP_SUMMARY"
