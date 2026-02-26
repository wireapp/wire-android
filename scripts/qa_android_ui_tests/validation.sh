#!/usr/bin/env bash
set -euo pipefail

# Validation and selector utilities used by qa-android-ui-tests workflow.

usage() {
  echo "Usage: $0 {validate-upgrade-inputs|resolve-selector-from-tags|print-resolved-values}" >&2
  exit 2
}

trim() {
  echo "$1" | xargs
}

validate_upgrade_inputs() {
  if [[ "${IS_UPGRADE:-}" == "true" && -z "${OLD_BUILD_NUMBER:-}" ]]; then
    echo "ERROR: oldBuildNumber is REQUIRED when isUpgrade=true"
    exit 1
  fi
}

resolve_selector_from_tags() {
  : "${GITHUB_OUTPUT:?GITHUB_OUTPUT not set}"

  local testcase_id=""
  local category=""
  local tags_raw="${TAGS_RAW:-}"

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
  echo "flavor=${FLAVOR_INPUT:-}"
  echo "resolvedTestCaseId=${RESOLVED_TESTCASE_ID:-}"
  echo "resolvedCategory=${RESOLVED_CATEGORY:-}"
}

case "${1:-}" in
  validate-upgrade-inputs)
    validate_upgrade_inputs
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
