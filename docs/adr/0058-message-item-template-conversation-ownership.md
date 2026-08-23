# ADR 0058: Move the message-item template into the conversation feature

**Status:** Accepted
**Baseline:** `6a0b2f368`, `chore/android-modularization`

## Decision

Move `MessageItemTemplate` package-preserving from `:app` to
`:features:conversation`. It is a pure Compose presentation template and retains
its public signature, Compose dependencies, and `:core:ui-common` dimensions
dependency unchanged.

## Consequences

There are no caller, import, Gradle, resource, route, or runtime changes. The
conversation boundary inventory verifies the preserved package and rejects
app-only imports, resources, and `BuildConfig` usage.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:testDebugUnitTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the template requires an app-only
dependency, resource, route, or Android-host behavior.
