# ADR 0027: Calling participant-count app adoption

**Status:** Accepted
**Baseline:** `985a5d58b`, `chore/android-modularization`

## Decision

Construct `KaliumObserveConversationParticipantCount` in the app-owned
conversation and meetings calling ViewModels with their injected Kalium
`ObserveConversationMembersUseCase`. Both `JoinOrStartCallManager` instances
therefore consume the neutral `:core:calling` producer.

`ConversationCallViewModel` retains
`ObserveParticipantsForConversationUseCase` only to update its own
`ConversationCallViewState.participantsCount`. `MeetingsCallViewModel` removes
the conversation participant aggregation dependency entirely; it no longer
imports `ConversationParticipantsData` or its use case.

## Boundary

No Metro graph, binding, Gradle, feature, resource, flavor, or behavior change
is made. The app remains the composition owner. Meetings reaches no
conversation-feature declaration: its count source is Kalium through the
neutral calling producer. The meeting start path continues to skip participant
count validation exactly as before.

## Verification and stop conditions

Verify with JDK 21:

```sh
./gradlew :app:testDevDebugUnitTest --rerun-tasks \
  --tests com.wire.android.ui.home.conversations.call.ConversationCallViewModelTest \
  --tests com.wire.android.ui.home.meetings.MeetingsCallViewModelTest
./gradlew :app:compileDevDebugKotlin :app:compileFdroidDebugKotlin
rg -n 'details\\.participants' \
  app/src/main/kotlin/com/wire/android/ui/home/meetings/MeetingsCallViewModel.kt \
  app/src/test/kotlin/com/wire/android/ui/home/meetings/MeetingsCallViewModelTest.kt
```

Stop rather than widening this atom if Metro requires a new binding or if any
meetings source needs a conversation-feature type. This ADR authorizes no
further calling or feature move.
