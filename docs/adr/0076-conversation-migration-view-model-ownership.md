# ADR 0076: Conversation migration ViewModel ownership

**Status:** Accepted

**Date:** 2026-08-23

**Baseline:** `81e805604`

## Context

`ConversationMigrationViewModel` and its focused test remained in `:app` after the
conversation facade became the owner of reusable presentation logic. The ViewModel
observes Kalium conversation details and exposes only the replacement conversation
ID when a one-to-one conversation has migrated. Its assisted argument was the
app-owned `ConversationNavArgs`, although it consumed only `conversationId`.

## Decision

Move `ConversationMigrationViewModel` and its test to `:features:conversation` with
unchanged packages and public FQNs. Replace the assisted navigation argument with the
narrow Kalium `ConversationId` contract while preserving observation, filtering, and
state-update behavior.

Create a feature-owned `ConversationMigrationViewModelGraph` with the dedicated
`ConversationMigrationManualViewModelFactoryGroup`, a no-argument lookup gateway, and
an assisted `ConversationId` gateway. The binding names
`conversationMigrationViewModel` explicitly as its factory method. App keeps a small
route adapter from `ConversationNavArgs` and installs the generated binding container
exactly once in `AppSessionViewModelGraph`.

Navigation 3 entries, route and screen contracts, resources, Gradle dependencies,
`ConversationModule`, and the remaining `ConversationCoreManualViewModelFactoryGroup`
stay unchanged.

## Consequences

Conversation migration presentation now has one feature owner without introducing an
app import or new dependency edge. Route callers keep their existing API, while the
feature-generated assisted factory exposes only the data it actually needs. The diff
remains move-first and the core factory continues to own its unrelated ViewModels.

## Stop conditions

Stop rather than broaden this extraction if it requires app resources, `BuildConfig`,
Navigation 3 runtime, a new module edge, a resource move, a changed route call, a
duplicate Metro binding, or any modification to unrelated core-factory methods.

## Verification

Use Java 21. Run the moved `ConversationMigrationViewModelTest`, the conversation
boundary test, the app assembly ownership test, `EntryOwnedViewModelGatewaySourceTest`,
`ConversationNavigation3SourceTest`, and `ModuleDependencyBoundaryTest`. Compile the
feature and app dev/fdroid variants sequentially. Inspect generated KSP sources to
confirm that the dedicated factory accepts `ConversationId`, the old core factory no
longer contains migration methods, and the new binding container is installed once.
Finish with source counts, old-path and forbidden-import audits, `git diff --check`,
and rename-similarity inspection.
