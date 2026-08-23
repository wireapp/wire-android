# ADR 0019: Neutral calling coordinator

**Status:** Accepted
**Baseline:** `cc124ec0d`, `chore/android-modularization`

## Context

`JoinOrStartCallManager` is used by conversation and by the app-hosted meetings
flow. Its former app location also made the meetings flow depend on conversation
implementation. The coordinator needs a participant count only; it does not need
conversation participant UI data.

## Decision

Create Android-only `:core:calling` and move, without package renaming:

- `JoinOrStartCallManager`, `JoinOrStartCallViewState`, and the
  `JoinOrStartCallScreenDialogType` contract;
- the `JoinOrStartCallViewActions` sealed contract; and
- `ObserveConversationParticipantCount`, a `ConversationId -> Flow<Int>` port.

Conversation and the app meetings host adapt their existing
`ObserveParticipantsForConversationUseCase` locally with `allCount`. The manager
retains its first-emission/default-zero behavior. `JoinOrStartCallRuntimeActions`
and `JoinOrStartCallRuntimeDialogs` stay in app: launching activities, recording
flavor-selected analytics, and rendering app-owned dialogs are runtime-adapter
behavior. The coordinator exposes only the five dialog-response methods needed by
that renderer (`joinAnyway`, `initiateCall`, the two start-call confirmations, and
`dismissDialog`); they were `internal` only while caller and coordinator shared the
app module.

`:core:calling` may use only `:core:ui-common`, Kalium common/logic, Compose,
coroutines, and visibility support proved by these moved files. Its public ABI
exposes `ActionsManager`, Kalium Logic types, and `Flow`, so `:core:ui-common`,
Kalium Logic, and coroutines use `api`; Kalium common, Compose, and AndroidX Core
(`VisibleForTesting`) remain implementation details. Metro compiler and automatic
runtime dependencies are disabled. It must not depend on app, features, navigation,
services, analytics implementations, AVS, or KSP.

Call activities/intents, services, AVS rendering, Metro bindings/groups/scopes,
routes/results, resources, and flavor selection remain unchanged in app.

## Verification and stop conditions

Compile `:core:calling`, app, and meetings; run the moved manager tests and the
calling boundary test. Stop if preserving behavior requires an app/feature edge,
Metro participation, or moving an activity/service/AVS runtime type. A future
feature may consume this core contract only through its own direct core edge; no
feature-to-feature edge is permitted.
