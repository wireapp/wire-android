# 16. Manual Dev distribution from branches and PRs

Date: 2026-10-12

## Status

Accepted. Supersedes [ADR 0014](0014-firebase-app-distribution-for-dev-builds.md).

## Context

Testers need branch builds and existing PR APKs. Keep this a small manual workflow rather than
adding a separate verification pipeline around Firebase uploads.

## Decision

Run the workflow from `develop`. Choose a repository `source-branch` (default `develop`) or a PR
number. Build branches with `make build-dev` and the shared debug signing configuration. For a
same-repository PR targeting `develop`, download the successful Develop CI APK for its head
revision at selection time. Missing or expired artifacts fail at download; there is no rebuild fallback.

Prepare one APK, transfer it to a separate upload job, and distribute it using the Firebase CLI.
Keep the Firebase key in a `firebase-dev` environment restricted to `develop`, with no accessible
repository/organization copy. Use a temporary credentials file, owner-only permissions, and exit
cleanup. Require the internal tester group and append source/CI details to release notes.

Use CI and Firebase's normal validation rather than custom package/certificate inspection,
artifact-expiration preflight, or a second PR-head check. PR builds are CI test merges; branch
builds receive an APK build only. Both must use the DevDebug package and shared signing identity.

## Consequences

The workflow retains PR-number convenience and the credential boundary with fewer moving parts.
It distributes the PR revision selected at the start even if the PR subsequently changes. Stable
package/signing are build-configuration requirements; we do not inspect them before upload.
GitHub environment/secret configuration remains required; updates to older version codes may
require reinstalling. See the [setup guide](../../scripts/dev/README_FIREBASE_DISTRIBUTION.md).
