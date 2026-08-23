# ADR 0025: Conversation participant developer capability

**Status:** Accepted
**Baseline:** `2cdb82217`, `chore/android-modularization`

## Decision

Remove the remaining direct
`BuildConfig.DEVELOPER_FEATURES_ENABLED` reads from conversation participant
renderers. The app-hosted value is read only at three route boundaries:
`GroupConversationDetailsRouteScreen`, `GroupConversationAllParticipantsRouteScreen`,
and `MessageDetailsRouteScreen`. Each boundary passes the Boolean explicitly to
its pure rendering and `LazyListScope` helpers, ending at
`ConversationParticipantItem`.

This preserves the developer MLS-progress header and per-participant protocol
labels without making feature-movable renderers depend on app `BuildConfig`.
There are no host-derived default arguments: public renderer/helper callers
must provide the capability, and previews explicitly select their value.

## Scope and boundary

No source is moved and no Gradle, Metro, Navigation3, resource, flavor, or
runtime behavior changes. `LocalConversationHostConfiguration` remains outside
pure helpers and lazy-list factories. The shared participant-item path includes
message-details reactions and read receipts, so those renderers receive the
same explicit capability rather than diverging from group-details behavior.

## Verification and stop conditions

Verify with JDK 21:

```sh
./gradlew :app:testDevDebugUnitTest --rerun-tasks \
  --tests com.wire.android.ui.home.conversations.details.GroupDetailsViewModelTest \
  --tests com.wire.android.ui.home.conversations.details.participants.GroupParticipantsViewModelTest
./gradlew :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin :app:compileFdroidDebugKotlin
```

Stop rather than widening this atom if another module needs a changed public
renderer signature, or if capability propagation requires a CompositionLocal
read in a lazy-list/pure helper. This ADR authorizes no further extraction.
