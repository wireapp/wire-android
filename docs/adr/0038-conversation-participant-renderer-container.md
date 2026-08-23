# ADR 0038: Move the group-participant renderer container into the conversation feature

**Status:** Accepted
**Baseline:** `68ca2ea3a`, `chore/android-modularization`

## Decision

Move `GroupConversationParticipants` package-preserving from `:app` to
`:features:conversation`.

The composable owns the participant-list container and MLS progress presentation.
Its data aggregate, expansion state, participant-list extension, and row renderer
are already conversation-feature sources. The app group-details screen continues
to invoke the same package-preserving public composable. The screen, navigation
contracts, and ViewModel remain in `:app`.

Keep the three Compose preview functions in the new app-owned,
same-package `GroupConversationParticipantsPreviews.kt`, analogous to
`ConversationParticipantItemPreviews.kt`. They retain the app-internal
`PreviewMultipleThemes` annotation and call the moved public composables over the
existing app-to-feature dependency. This preserves tooling previews without
exposing the deliberately internal helper or creating a feature-to-app edge.

## Consequences

No resources, Gradle dependencies, Metro bindings, route wiring, or
feature-to-feature edges are added. The existing conversation feature dependency
already exposes Compose runtime, Material 3, Kalium Logic, and shared UI types.
The boundary test now treats the container as a feature-owned, legacy-package
source and rejects imports from `:app` implementation packages.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.participants.GroupParticipantsViewModelTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the container needs an app-only resource,
Metro binding, navigation type, or a feature-to-app dependency.
