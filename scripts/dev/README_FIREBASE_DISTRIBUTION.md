# Distribute Dev builds

For a branch, run **Distribute Dev Build** from `develop`, choose `source-branch` (default `develop`),
and provide release notes. The manual form is for branches only.

For a PR, post a new comment containing exactly `/distribute-dev`. Repository write access or higher
is required. The shortcut uses the PR title as release notes and promotes its current successful
Develop CI APK. A thumbs-up reaction means the request was accepted; thumbs down means it was
rejected. Check Actions for the final upload result. Editing an existing comment does not trigger it.
Both paths add source and CI details to the release notes.

## One-time setup

- Create `firebase-dev`, allowing deployments only from the branch `develop`.
- Store `FIREBASE_APP_DISTRIBUTION_SERVICE_ACCOUNT_JSON` in that environment. Remove any accessible
  repository or organization copy after saving the environment copy.
- Set `FIREBASE_APP_DISTRIBUTION_GROUPS` to the internal tester group aliases (comma-separated).
- Use the existing shared debug signing secrets and set `ENABLE_SIGNING=TRUE` for PR CI. Branch
  distribution enables it explicitly. Both paths must produce `com.wire.android.dev.debug`.
- Run preparation and upload jobs on fresh runners so build code cannot access upload credentials.
- Keep `develop` as the default branch; the comment workflow must be present there to receive events.

PRs must belong to this repository and target `develop`. If their APK is missing or expired,
rerun PR CI. Branch builds do not run the full PR quality gates. Package/signing configuration
must match between sources; installing an older version code may require a reinstall.

The design is recorded in [ADR 0018](../../docs/adr/0018-trusted-dev-distribution-from-branches-and-prs.md).
