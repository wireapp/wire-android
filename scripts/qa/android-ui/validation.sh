#!/usr/bin/env bash
set -euo pipefail

# Validation and selector utilities used by qa-android-ui-tests workflow.

usage() {
  echo "Usage: $0 {validate-upgrade-inputs|validate-rerun-inputs|resolve-selector-from-tags|print-resolved-values}" >&2
  exit 2
}

trim() {
  # Normalise manual workflow input so validation does not depend on spacing.
  echo "$1" | xargs
}

validate_upgrade_inputs() {
  # oldBuildNumber is optional for upgrade runs. When empty, APK selection uses
  # the previous APK as the old version and the selected appBuildNumber as new.
  :
}

validate_rerun_inputs() {
  local enabled="${RERUN_FAILED_ENABLED:-true}"
  local count="${RERUN_FAILED_COUNT:-1}"
  local count_num=0

  if [[ ! "${enabled}" =~ ^(true|false)$ ]]; then
    echo "ERROR: rerunFailedEnabled must be true or false."
    exit 1
  fi

  if [[ ! "${count}" =~ ^[0-9]+$ ]]; then
    echo "ERROR: rerunFailedCount must be a whole number >= 0."
    exit 1
  fi

  count_num=$((10#${count}))
  # Cap reruns so one flaky run cannot monopolise the shared device pool.
  if (( count_num > 3 )); then
    echo "ERROR: rerunFailedCount must be <= 3."
    exit 1
  fi
}

resolve_selector_from_tags() {
  : "${GITHUB_OUTPUT:?GITHUB_OUTPUT not set}"

  local testcase_id=""
  local category=""
  local tags_raw="${TAGS_RAW:-}"

  # CI currently supports one selector per run: either a Test Case ID or a
  # category. If multiple tags are typed, use the first non-empty token.
  if [[ -n "$(trim "${tags_raw}")" ]]; then
    local sel=""
    IFS=',' read -ra parts <<< "${tags_raw}"
    for p in "${parts[@]}"; do
      local t
      t="$(trim "$p")"
      if [[ -n "$t" ]]; then
        sel="$t"
        break
      fi
    done

    sel="${sel#@}"
    sel="$(trim "$sel")"

    # Leave key:value tags unsupported here so rerun/filter semantics stay
    # simple until CI has a dedicated contract for that mode.
    if [[ "$sel" == *:* ]]; then
      echo "ERROR: TAGS format '@key:value' is not supported yet. Use '@TC-1234' or '@category'."
      exit 1
    fi

    if [[ "$sel" =~ ^TC-[0-9]+$ ]]; then
      testcase_id="$sel"
    else
      category="$sel"
    fi
  fi

  echo "testCaseId=${testcase_id}" >> "$GITHUB_OUTPUT"
  echo "category=${category}" >> "$GITHUB_OUTPUT"
}

print_resolved_values() {
  echo "workflowRef=${WORKFLOW_REF:-}"
  echo "flavor=${FLAVOR_INPUT:-}"
  echo "resolvedIsUpgrade=${IS_UPGRADE:-}"
  echo "resolvedTestCaseId=${RESOLVED_TESTCASE_ID:-}"
  echo "resolvedCategory=${RESOLVED_CATEGORY:-}"
  echo "testinyRunName=${TESTINY_RUN_NAME:-}"
}

case "${1:-}" in
  validate-upgrade-inputs)
    validate_upgrade_inputs
    ;;
  validate-rerun-inputs)
    validate_rerun_inputs
    ;;
  resolve-selector-from-tags)
    resolve_selector_from_tags
    ;;
  print-resolved-values)
    print_resolved_values
    ;;
  *)
    usage
    ;;
esac
