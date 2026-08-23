# ADR 0063: Own message details empty text in conversation

**Status:** Accepted
**Baseline:** `61ea48c2d`, `chore/android-modularization`

## Decision

Move `MessageDetailsEmptyScreenText` from `:app` to `:features:conversation`
while preserving the existing
`com.wire.android.ui.home.conversations.messagedetails` package and public
behaviour. No caller imports change.

The composable depends only on Compose and shared UI-common theme helpers. It
has no app resource, BuildConfig, runtime binding, navigation, Metro, or hidden
app dependency.

## Consequences

The stable FQN remains available to app callers through the existing
`:app` to `:features:conversation` dependency. The move adds no Gradle edge and
does not change resource ownership, runtime composition, or navigation.

The conversation module boundary inventory protects the physical feature owner,
the legacy package, and the absence of app implementation imports.

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

Stop rather than widening this slice if the move requires an app resource,
runtime binding, navigation type, Metro integration, or hidden app dependency.
