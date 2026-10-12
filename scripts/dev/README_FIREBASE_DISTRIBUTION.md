# Distribute Dev builds

The intended source and trust policy is recorded in [ADR 0016](../../docs/adr/0016-trusted-dev-distribution-from-branches-and-prs.md).

## Required GitHub configuration

Before using the workflow:

1. Create the `firebase-dev` environment and configure selected deployment branches to allow only
   the branch `develop`, excluding tags. Keep changes to that branch under the repository's review policy.
2. Put `FIREBASE_APP_DISTRIBUTION_SERVICE_ACCOUNT_JSON` in this environment. Remove any repository
   or organization secret with that name accessible to this repository after the environment copy
   is configured. Otherwise branch workflows could still access the long-lived key.
3. Set `FIREBASE_APP_DISTRIBUTION_GROUPS` to the internal Firebase tester group aliases (comma-separated),
   either as a repository variable or a `firebase-dev` environment variable.
4. Configure the shared debug signing secrets used by existing PR CI:
   `ENCODED_KEYSTORE_DEBUG`, `SIGNING_KEY_ALIAS_DEBUG`, `SIGNING_KEY_PASSWORD_DEBUG`,
   and `SIGNING_STORE_PASSWORD_DEBUG`. Set `ENABLE_SIGNING=TRUE` for PR CI. Distribution rejects
   PR artifacts signed with a different key; rerun CI after changing signing configuration.
5. Ensure the upload runner is fresh for each job and has Android SDK build tools available through
   `ANDROID_HOME`. Separate jobs on persistent shared hosts do not provide the intended isolation.

The Firebase key needs Firebase App Distribution permissions for the intended project. No real key
belongs in the checkout, build artifacts, or logs.

## Dispatch

In GitHub Actions, open **Distribute Dev Build** and keep **Use workflow from** set to `develop`.
Use one of:

- Set `source-branch` to a repository branch. The workflow resolves and builds its current commit.
- Set `pull-request` to a same-repository PR targeting `develop`. The workflow promotes the available
  APK from successful CI for the current PR revision; it does not rebuild it.
- Leave both blank to build `develop`.

Provide release notes describing what testers should exercise. The workflow adds source identity,
CI run URL, and validation level, and grants access to the configured internal tester group.

If the PR build is missing, failed, expired, or signed differently, rerun PR CI and dispatch again.
If the PR changes while distribution is preparing, dispatch again for its new revision. Older builds
are deliberately not selected. PR APKs represent GitHub's test merge into the base branch;
branch APKs represent the selected branch directly.
