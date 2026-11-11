#!/usr/bin/env bash
# Print the review prompt for a Claude review workflow.
#
# Inputs (environment):
#   TIER      "1" for a light review, anything else for a deep review
#   REASONS   why the classifier chose the tier, one line per reason
#   REPO      owner/name
#   PR_NUMBER pull request number
#   CODE_DIR  directory that holds the PR code, relative to the working directory.
#             Empty when the PR code is not checked out. Then Claude reads the diff only.
#   DELIVERY  "inline"        post inline comments, the final message is the summary
#             "summary"       post nothing, the final message holds the review
#             "comment:<id>"  update the existing PR comment with this id
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
: "${TIER:?}" "${REPO:?}" "${PR_NUMBER:?}" "${DELIVERY:?}"
code_dir=${CODE_DIR:-}
reasons=${REASONS:-"none given"}

cat "$here/common.md"
echo
if [ "$TIER" = "1" ]; then
  cat "$here/light.md"
  label=light
else
  cat "$here/deep.md"
  label=deep
fi

if [ -n "$code_dir" ]; then
  code_line="The PR code is in the directory \"$code_dir\". Open files there. Scope every Grep and Glob to that directory."
else
  code_line="The PR code is not checked out. Read it with gh pr diff and gh pr view. Say under \"Not verified:\" what you could not open."
fi

cat <<EOF

## Context

Repository: $REPO
Pull request: #$PR_NUMBER
$code_line

Review tier: $label. The classifier gave these reasons:
$reasons
EOF
echo

case "$DELIVERY" in
  inline)
    cat <<EOF
## Delivery

Before you post, read the existing review comments with "gh pr view $PR_NUMBER --comments". Skip a finding that a comment already reports.
Post each remaining finding on its line with the inline comment tool.
Your final message is the summary comment. Begin it with "Review tier: $label" and the reasons above. Then list the findings, most severe first.
EOF
    ;;
  summary)
    cat <<EOF
## Delivery

Do not post comments. Write the review in your final message only.
Begin it with "Review tier: $label" and the reasons above. Then list the findings, most severe first.
EOF
    ;;
  comment:*)
    id=${DELIVERY#comment:}
    case "$id" in (*[!0-9]*|"") echo "bad comment id" >&2; exit 1;; esac
    cat <<EOF
## Delivery

A comment (ID $id) already says the review is in progress. Do not create a new comment. Update it with the review:

gh api repos/$REPO/issues/comments/$id -X PATCH -f body="<your review>"

Begin the review with "Review tier: $label" and the reasons above. Then list the findings, most severe first.
EOF
    ;;
  *)
    echo "bad DELIVERY" >&2
    exit 1
    ;;
esac
