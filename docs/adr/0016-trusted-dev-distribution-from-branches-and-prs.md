# 16. Trusted Dev distribution from branches and PRs

Date: 2026-10-12

## Status

Accepted. Supersedes [ADR 0014](0014-firebase-app-distribution-for-dev-builds.md).

## Context

Internal testers need both branch builds and the APK already produced by successful Develop PR CI.
Selecting a branch through GitHub's workflow picker also selects that branch's workflow code.
Building arbitrary branch code in the runner that later receives Firebase credentials weakens the
credential boundary. The two paths also need compatible signing and clear source information.

## Decision

Keep Dev distribution manual and separate from the shared build workflow. Run the distribution
workflow from `develop`, using an explicit `source-branch` input or a mutually exclusive PR number.
With neither input, build `develop`. Only branches in this repository and same-repository PRs
targeting `develop` are eligible; fork PRs are excluded.

Resolve a branch to a commit before checkout and require its APK build to succeed. For a PR, select
the successful Develop CI run associated with that PR's current head revision and promote its
existing test-merge APK. Fail when no matching successful run exists or the artifact is unavailable;
never fall back to an older revision or silently rebuild. Recheck the PR head before upload.
The PR binary represents the merge tested by CI, while the branch binary represents branch head.

Prepare the APK and upload it in separate jobs on fresh runners. The upload job uses trusted
workflow configuration, executes no selected-source scripts, and receives only the prepared APK
and source metadata. A `firebase-dev` GitHub environment must allow deployments only from
`develop`. Store the Firebase service-account secret in that environment, removing any copy at
repository or organization scope that is accessible to this repository. A workflow guard alone
cannot enforce this boundary against edited workflows on other branches.

Continue using the Firebase CLI with a temporary credentials file, owner-only permissions,
Application Default Credentials, and exit cleanup. Short-lived credentials remain a possible
future change; this decision retains the existing service-account integration.

Both paths use the shared debug signing identity and DevDebug package
`com.wire.android.dev.debug`. Validate the APK package and signing certificate against trusted
configuration before upload. Branch builds enable shared signing explicitly; PR CI must enable it
as well. Require a configured internal tester group. Include source label, source SHA, CI run URL,
and validation level alongside the operator's release notes.

## Consequences

- Testers can distinguish a branch build from the exact PR binary that passed CI.
- Branch distributions require an APK build; PR distributions require the existing successful
  Develop CI workflow, including its current quality gates and deployment steps.
- PR artifact reuse is limited by the existing seven-day retention. Missing artifacts require a new
  PR CI run, not a different source revision.
- Compatible package and signing permit normal updates between sources. Android version-code
  ordering still applies; installing an older build may require a reinstall.
- Firebase environment protection and secret placement require GitHub configuration. The code
  does not make a repository-wide Firebase secret safe for arbitrary workflow branches.
- Fresh runner isolation is required. Branch build code still receives the debug signing credentials
  and build-service secrets it needs; the boundary protects the Firebase upload credentials.
- The upload runner needs Android SDK build tools for package and signature verification.

## References

- [GitHub workflow revision selection](https://cli.github.com/manual/gh_workflow_run)
- [GitHub PR test-merge behavior](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#pull_request)
- [GitHub Actions secure use](https://docs.github.com/en/actions/reference/security/secure-use)
- [Firebase service-account authentication](https://firebase.google.com/docs/app-distribution/authenticate-service-account?platform=android)
