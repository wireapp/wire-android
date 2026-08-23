# ADR 0061: Move the intercept-clickable modifier into the conversation feature

**Status:** Accepted
**Baseline:** `3a953f231`, `chore/android-modularization`

## Decision

Move `interceptCombinedClickable` package-preserving from `:app` to
`:features:conversation`. It is a pure Compose gesture modifier and retains its
public signature and press, click, long-press, cancellation, and indication
behavior unchanged.

## Consequences

There are no caller, import, Gradle, resource, route, or runtime changes. The
conversation boundary inventory verifies the preserved package and rejects
app-only imports, resources, and `BuildConfig` usage. The modifier has only
Compose dependencies, so it adds no feature allowlist entries.

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

Stop rather than widening this slice if the modifier requires an app-only
dependency, resource, route, or Android-host behavior.
