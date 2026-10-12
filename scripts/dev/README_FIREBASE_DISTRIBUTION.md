# Distribute Dev builds

Run **Distribute Dev Build** from `develop`. Set `source-branch` to build a repository branch, or
`pull-request` to reuse its current successful Develop CI APK. Leave both blank to build `develop`.
Provide release notes; the workflow adds source and CI details.

## One-time setup

- Create `firebase-dev`, allowing deployments only from the branch `develop`.
- Store `FIREBASE_APP_DISTRIBUTION_SERVICE_ACCOUNT_JSON` in that environment. Remove any accessible
  repository or organization copy after saving the environment copy.
- Set `FIREBASE_APP_DISTRIBUTION_GROUPS` to the internal tester group aliases (comma-separated).
- Use the existing shared debug signing secrets and set `ENABLE_SIGNING=TRUE` for PR CI. Branch
  distribution enables it explicitly. Both paths must produce `com.wire.android.dev.debug`.
- Run preparation and upload jobs on fresh runners so build code cannot access upload credentials.

PRs must belong to this repository and target `develop`. If their APK is missing or expired,
rerun PR CI. Branch builds do not run the full PR quality gates. Package/signing configuration
must match between sources; installing an older version code may require a reinstall.

The design is recorded in [ADR 0016](../../docs/adr/0016-trusted-dev-distribution-from-branches-and-prs.md).
