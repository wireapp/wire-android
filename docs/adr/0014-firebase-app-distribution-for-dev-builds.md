# 14. Firebase App Distribution for Dev builds

Date: 2026-08-20

## Status

Accepted

## Context

We need to distribute DevDebug APKs to testers without adding App Distribution uploads to the
shared build workflow. Firebase App Distribution requires the APK package name to match the
selected Firebase Android app.

## Decision

Use the Firebase App Distribution Gradle plugin for the Dev flavor only. It is enabled only when
CI provides `FIREBASE_APP_DISTRIBUTION_SERVICE_ACCOUNT_JSON`; local builds do not create upload
tasks.

Use a manual `Distribute Dev Build` workflow for uploads. The workflow accepts a PR number to reuse
the existing DevDebug artifact from its successful Develop PR build. Without a PR number, it builds
DevDebug from the branch selected when the workflow is dispatched.

Dev's base application ID is `com.wire.android.dev`, so DevDebug uses
`com.wire.android.dev.debug`, matching the Firebase Android app used for distribution.

## Consequences

- App Distribution is opt-in and does not alter the shared build workflow.
- The Dev package-ID change does not change `firebase_app_id`, `firebase_push_sender_id`, or the
  Firebase project used for push notifications.
- Existing `com.waz.zclient.dev.debug` installations do not update in place and should be treated
  as ephemeral Dev installations.
