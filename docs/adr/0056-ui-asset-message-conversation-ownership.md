# ADR 0056: Move the UI asset-message presentation model into the conversation feature

**Status:** Accepted
**Baseline:** `9bcfd55a8`, `chore/android-modularization`

## Decision

Move `UIAssetMessage` package-preserving from `:app` to
`:features:conversation`. It is a pure presentation model: its public shape and
dependencies on `UIText`, Kalium `QualifiedID`, `Instant`, and `Path` remain
unchanged.

## Consequences

There are no caller, import, Gradle, resource, route, or runtime changes. The
feature boundary inventory verifies the preserved package and rejects app-only
imports, resources, and `BuildConfig` usage.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:testDebugUnitTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the model requires an app-only
dependency, resource, route, or Android-host behavior.
