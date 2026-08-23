# ADR 0036: Move conversation participant renderers into the conversation feature

**Status:** Accepted
**Baseline:** `5a1eec28f`, `chore/android-modularization`

## Decision

Move `ConversationParticipantItem` and `GroupConversationParticipantList`
package-preserving from `:app` to `:features:conversation`. Move the three
section-label resources used only by those renderers with them:
`conversation_details_conversation_admins`,
`conversation_details_conversation_members`, and
`conversation_details_conversation_apps`. Preserve all four existing resource
qualifiers, text values, and default translatability.

The renderers depend on feature-owned participant presentation state and data, and
on neutral UI primitives from `:core:ui-common`. Their query highlighting is the
only direct dependency not already exposed by the feature, so
`:features:conversation` adds `implementation(projects.core.search)`. This is a
core utility edge, not a feature-to-feature or feature-to-app dependency.

The renderer continues to resolve shared `you`, temporary-user, and empty-content
labels through the neutral `core:ui-common` resource namespace. Conversation-only
section labels resolve through the feature resource namespace.

## Consequences

The package names and public renderer APIs remain unchanged, so the app's existing
screen host keeps compiling without call-site changes. There is no change to
navigation, ViewModel ownership, Metro bindings, resource behavior, or the
participant list's ordering and expansion behavior.

The three Compose previews remain app-owned in
`ConversationParticipantItemPreviews.kt`, in the same Kotlin package. They use the
app-internal preview annotation while calling the public renderer from the feature,
so module ownership changes without losing development tooling or exposing an app
annotation from the feature.

`GroupConversationParticipants.kt` deliberately remains in `:app`: it is the
screen-level host and is outside this renderer-only slice. Moving it would widen
the change from an ownership relocation into a navigation and composition audit.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=100% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --tests com.wire.android.feature.conversation.ConversationParticipantRendererResourceOwnershipTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if a renderer requires an app-only resource,
navigation or Metro contract, or another feature dependency.
