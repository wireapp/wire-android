# ADR 0077: Conversation message-user resolution ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `82f90fbb8`

## Context

`GetUsersForMessageUseCase` and its focused test remained in `:app`, although the use
case supplies conversation message presentation and has no Android runtime
responsibility. Its only app-owned dependency was `MessageMapper.memberIdList`, a
projection unrelated to the mapper's remaining UI-message responsibility.

The projection is behavior-sensitive. It keeps the sender first when available, then
loads users referenced by partial-delivery failures, member-change events, or
legal-hold member events. Partial delivery lists failed-delivery recipients before
recipients without clients, and duplicate IDs retain their first-seen position.

## Decision

Move `GetUsersForMessageUseCase` and its focused test to `:features:conversation` with
their package and public FQN unchanged. Keep `ObserveUserListByIdUseCase` as the only
injected dependency and move the exact single-message additional-user projection into
the use case as private implementation.

Remove `MessageMapper.memberIdList`, its direct mapper test, and the unused stubs from
`GetConversationMessagesFromSearchUseCaseTest`. `MessageMapper` remains app-owned for
mapping Kalium messages to app UI models. Update the generated-factory descriptors in
the app baseline and startup profiles from two providers to one.

No Gradle dependency, module, resource, navigation, Metro group, public contract, or
module-graph edge changes in this slice. Existing app consumers continue to resolve
the package-preserved use case through the feature dependency.

## Behavioral contract

- Sender inclusion continues to use `message.sender` only and precedes observed users.
- Complete deliveries contribute no additional IDs.
- Partial deliveries contribute failed-delivery IDs before failed-with-no-clients IDs.
- All member changes and both legal-hold-for-members variants contribute their members.
- Other system messages and signaling messages contribute no IDs.
- IDs are distinct in first-seen order; the sender is not deduplicated against them.
- The user observer is invoked only for a non-empty additional-ID list, and its first
  emission is consumed exactly as before.

## Stop conditions

Stop rather than broaden the extraction if it requires an app import in the feature,
a new dependency edge, a shared mapper abstraction, a changed public FQN, different
sender or ordering semantics, or moving the app UI-message mapper itself.

## Consequences

Conversation message-user resolution has a single feature owner and the app mapper
loses an unrelated responsibility. The review remains dominated by two file moves,
narrow mapper/test deletions, a boundary inventory addition, and deterministic profile
signature updates.

## Verification

Use Java 21 to run the moved `GetUsersForMessageUseCaseTest` and
`ConversationModuleBoundaryTest`, plus app `MessageMapperTest`, message-search, quote,
observe-message, and module-boundary tests. Compile `:features:conversation`, then app
dev and fdroid variants sequentially.

Inspect generated KSP output to prove that exactly one feature-owned
`GetUsersForMessageUseCase$MetroFactory` exists with one provider and that app output
contains no stale factory. Confirm no Kotlin source still calls `memberIdList`, the old
source/test paths are absent, the documented file counts match, and finish with
`git diff --check` and rename-similarity inspection.
