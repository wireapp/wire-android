#!/usr/bin/env bash
# Classify the diff between two commits of the current repository.
#
# Usage: classify-pr.sh <base-sha> <head-sha> <author>
#
# Prints the JSON that classify.py prints. Run it from the repository root.
# If the diff moves the kalium pointer, the script clones Kalium without file
# contents and fetches both commits. Set KALIUM_REPO to reuse an existing
# checkout. If Kalium cannot be fetched, classify.py treats the bump as deep.
set -euo pipefail

base=$1
head=$2
author=$3

here=$(cd "$(dirname "$0")" && pwd)
work=$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/review-tier.XXXXXX")
trap 'rm -rf "$work"' EXIT
diff_file="$work/pr.diff"

git diff --full-index "$base...$head" > "$diff_file"

kalium_args=()
pointer=$(sed -n '/^diff --git a\/kalium b\/kalium$/{n;s/^index \([0-9a-f]\{40\}\)\.\.\([0-9a-f]\{40\}\).*/\1 \2/p;}' "$diff_file")
if [ -n "$pointer" ]; then
  read -r old new <<<"$pointer"
  if [ -n "${KALIUM_REPO:-}" ]; then
    kalium_args=(--kalium-repo "$KALIUM_REPO")
  else
    url=$(git config -f .gitmodules submodule.kalium.url || true)
    kalium_dir="$work/kalium"
    if [ -n "$url" ] && git clone --quiet --filter=blob:none --no-checkout "$url" "$kalium_dir" \
      && git -C "$kalium_dir" fetch --quiet origin "$old" "$new"; then
      kalium_args=(--kalium-repo "$kalium_dir")
    fi
  fi
fi

python3 "$here/classify.py" --diff "$diff_file" --author "$author" ${kalium_args[@]+"${kalium_args[@]}"}
