# 19. Build Dev PR APKs on request

Date: 2026-11-03

## Status

Accepted. Supersedes the PR APK source in [ADR 0018](0018-trusted-dev-distribution-from-branches-and-prs.md).

## Context

PR CI built a Dev-Debug APK and uploaded it to S3 on every PR. The build repeated what
`connectedDevDebugAndroidTest` already compiles. It delayed green checks, and each PR needed an APK only
occasionally.

## Decision

Develop PR CI and the merge queue run only Lint, Detekt, Compose Stability, Unit Tests, and UI Tests.
They do not build Dev-Debug and do not upload to S3.

For a same-repository PR to `develop`, `/distribute-dev` from a user with write access verifies a
successful Develop CI run for the PR head SHA. It then builds that exact SHA with `make build-dev`
and the shared debug signing configuration. A separate job in the `firebase-dev` environment
uploads the APK to Firebase. If Develop CI has not passed for the head SHA, the request fails.

This replaces the ADR 0018 rule that downloads the Develop CI APK and has no rebuild fallback.
Push, `main`, beta, release candidate, and production builds do not change.

## Consequences

The first distribute after a push takes a full build, about 10 to 20 minutes. PR CI finishes
sooner and stores no APK.

The APK comes from the PR head, not from the CI test merge. Signing and secret-dependent
configuration problems appear when you distribute, not when you open the PR. Branch protection does
not require the build job. See the [setup guide](../../scripts/dev/README_FIREBASE_DISTRIBUTION.md).
