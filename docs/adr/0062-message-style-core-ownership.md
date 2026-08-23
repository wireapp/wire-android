# ADR 0062: Own message style in core UI common

**Status:** Accepted
**Baseline:** `2faf7d3ad`, `chore/android-modularization`

## Decision

Move `MessageStyle` and its presentation helpers from `:app` to
`:core:ui-common` while preserving the existing
`com.wire.android.ui.home.conversations.messages.item` package and public
behaviour. No caller imports change.

The type depends only on Compose and the shared core UI-common theme helpers
`colorsScheme` and `wireColorScheme`; it has no app resource, BuildConfig,
logging, Metro, navigation, or feature dependency.

## Consequences

The stable FQN remains available to app code and to the conversation feature
through its existing core UI-common dependency. This is a neutral prerequisite
for later message UI extraction and adds no Gradle edge or resource ownership
change.

Focused tests protect the bubble classification, opacity values, stable package,
physical core owner, and absence of app-only dependencies.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:testDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.messages.item.MessageStyleTest \
  --tests com.wire.android.ui.home.conversations.messages.item.MessageStyleOwnershipSourceTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:compileDebugKotlin \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the move requires an app-owned theme,
resource, runtime binding, navigation type, or Gradle dependency.
