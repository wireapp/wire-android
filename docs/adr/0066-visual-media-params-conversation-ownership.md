# ADR 0066: Own visual media parameters in conversation

**Status:** Accepted
**Baseline:** `b59e89888`, `chore/android-modularization`

## Decision

Move `VisualMediaParams`, `MaxBounds`, `NormalizedSize`, and the `size` helper
from `:app` to `:features:conversation` while preserving the existing
`com.wire.android.ui.home.conversations.model.messagetypes.image` package,
public API, serialization contract, imports, and behaviour. No caller imports
change.

The source depends only on Compose, shared UI-common dimensions, and
`kotlinx.serialization`, all of which are already available to the conversation
feature. It has no app resource, BuildConfig, runtime binding, navigation,
Metro, or hidden app dependency.

## Consequences

The stable FQNs and `@Serializable` declaration remain available to app callers
through the existing `:app` to `:features:conversation` dependency. The move
adds no Gradle edge and does not change resources, runtime composition,
navigation, serialized property names, or normalized-size behaviour.

The conversation module boundary inventory protects the physical feature owner,
the legacy package, and the absence of app implementation imports. Existing app
instrumented tests continue to exercise the normalization behavior without
caller changes.

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
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :app:connectedDevDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.wire.android.ui.home.conversations.model.messagetypes.image.VisualMediaParamsTest
```

Stop rather than widening this slice if the move requires an app resource,
runtime binding, navigation type, Metro integration, or hidden app dependency.
