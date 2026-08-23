# ADR 0075: Conversation call ViewModel ownership

**Status:** Accepted

**Date:** 2026-08-23

**Baseline:** `09c23398d`

## Context

`ConversationCallViewModel`, its assisted Metro gateway, and its focused test remained
in `:app` after the neutral call coordinator and participant-count producer moved to
`:core:calling`. The ViewModel owns conversation-specific call presentation state and
orchestration, so app ownership obscured the intended feature boundary. Its assisted
argument was the app-owned `ConversationNavArgs`, even though only `conversationId`
was consumed.

The runtime closure is intentionally different. `JoinOrStartCallRuntimeActions`
launches Android call activities and records analytics, while
`JoinOrStartCallRuntimeDialogs` renders host dialogs. Those are app runtime adapters,
not reusable conversation presentation implementation.

## Decision

Move `ConversationCallViewModel`, `ConversationCallViewModelGraph`, and
`ConversationCallViewModelTest` to `:features:conversation` with unchanged packages
and public FQNs. Replace the assisted app navigation argument with the narrow Kalium
`ConversationId` contract. Preserve the public `conversationId: QualifiedID`, call
state behavior, participant observation, ongoing-call validation, calling-enabled
event, and `ConversationCallManualViewModelFactoryGroup` identity.

App retains a route-facing `conversationCallViewModel(ConversationNavArgs)` adapter
that delegates only `args.conversationId` to the feature gateway. Navigation entries,
screens, route contracts, and `AppSessionViewModelGraph` therefore keep their existing
call shape and one-time generated binding installation. The two runtime adapter files
remain app-owned and unchanged.

Declare `api(projects.core.calling)` from `:features:conversation`. This is deliberately
an API dependency: the public `ConversationCallViewModel.callManager` property exposes
the `JoinOrStartCallManager` type from `:core:calling`, so consumers need it on the
feature's compile ABI.

## Internal-module decision

Do not create `:features:conversation:calling` in this slice. The ViewModel still uses
the facade-owned `ObserveParticipantsForConversationUseCase`. Moving it into an
internal calling module now would require an internal-module-to-facade dependency or
duplicate participant logic, both contrary to the topology. Reassess after participant
ownership provides a narrow internal capability or neutral port with an acyclic edge.

## Stop conditions

Stop rather than broaden the move if the feature needs app `R`, `BuildConfig`,
`ConversationNavArgs`, Navigation 3 runtime, an activity, service, analytics runtime,
or either `JoinOrStartCallRuntime*` adapter. Also stop on a new feature-to-feature edge,
a changed Metro group or generated binding identity, duplicate or missing session
installation, altered call behavior, or an additional dependency beyond the audited
`:core:calling` API edge and existing feature dependencies.

## Consequences

Conversation call presentation and its test now have one feature owner. App remains the
composition and Android side-effect boundary. The canonical graph changes the
conversation-to-calling edge from proposed to current, while the proposed meetings
edge remains independent.

The package-preserving move keeps screen and route imports stable. The only production
adaptation is the narrow app argument bridge, making the review primarily file moves
and assisted-argument replacement.

## Verification

Use Java 21 and clean feature/app outputs before compilation. Run the seven moved
`ConversationCallViewModelTest` cases, `ConversationModuleBoundaryTest`, app
`CallingViewModelAssemblyOwnershipSourceTest`, and `ModuleDependencyBoundaryTest`.
Compile `:features:conversation`, then app dev and fdroid variants sequentially.

Inspect KSP output to prove that exactly one feature-generated ConversationCall
assisted factory accepts `ConversationId`, app generates no stale duplicate, and
`ConversationCallManualViewModelFactoryMetroBindings` is installed exactly once in
the app session graph. Finish with public-ABI inspection, baseline comparison of the
two runtime adapter files, old-path and forbidden-import audits, `git diff --check`,
and rename-similarity inspection.
