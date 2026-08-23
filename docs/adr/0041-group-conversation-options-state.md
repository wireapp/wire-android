# ADR 0041: Own group conversation options state in the conversation feature

**Status:** Accepted
**Baseline:** `6641d3489`, `chore/android-modularization`

## Decision

Move `GroupConversationOptionsState` package-preserving from `:app` to
`:features:conversation`, together with its focused unit test. The state is a
presentation contract for the group-conversation options flow and belongs with
the conversation feature rather than the app shell.

The state keeps its Kotlin package
`com.wire.android.ui.home.conversations.details.options`. Its only
non-conversation presentation dependencies are the neutral channel-access
types, `ChannelAccessType` and `ChannelAddPermissionType`, which are already
owned by `:core:ui-common` as established by ADR 0040. This move therefore
does not introduce a feature-to-feature or feature-to-app dependency.

## Consequences

The app remains the owner of the host, navigation, UI composition, and
side-effect wiring. The conversation feature owns the immutable options-screen
state and its domain-facing values. Consumers retain the same imports and
runtime contract because the Kotlin package and class names do not change.

The move is deliberately limited to source ownership. It does not alter
Gradle dependencies, resources, Metro bindings, navigation routes, or runtime
behaviour.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.GroupConversationOptionsStateTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the state needs an app-only resource,
host-only configuration, feature-specific UI dependency, or a new module edge.
