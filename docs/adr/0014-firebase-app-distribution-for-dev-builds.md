# 14. Firebase App Distribution for Dev builds

Date: 2026-08-20

## Status

Accepted

## Context

Firebase App Distribution validates an APK's package name against the selected Firebase Android app.
The DevDebug APK used `com.waz.zclient.dev.debug`, while the existing Firebase app used for Dev
Firebase Cloud Messaging (FCM) is registered as `com.wire.android.dev.debug` for staging.

The existing Dev Firebase App ID and sender ID are runtime FCM configuration. Changing them solely
to upload an APK would risk changing push-notification behavior in staging.

## Decision

Dev's base application ID is `com.wire.android.dev`, so DevDebug builds use
`com.wire.android.dev.debug` and match the existing Firebase Android app.

The Firebase App Distribution Gradle plugin is enabled only when CI provides its service-account
JSON, and is configured for the Dev flavor only.

Distribution is performed by a manual GitHub Actions workflow. It can reuse the DevDebug artifact
from a successful Develop PR build, or build DevDebug from the manually selected branch.

## Consequences

- App Distribution and runtime FCM use the same Firebase Android app without an additional App ID
  variable.
- Existing `com.waz.zclient.dev.debug` installations do not update in place; Dev builds are ephemeral anyway.
- DevRelease and other non-Debug Dev variants use `com.wire.android.dev` and are not distribution
  targets unless a matching Firebase app is registered.
