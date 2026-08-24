# ADR 0108: Move conversation-asset messages state to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `1d90e9f228b4642a1afaeaf11865571a9b1dd03b`

## Context

`ConversationAssetMessagesViewModel` and its state now depend entirely on feature-owned media
arguments/paging use cases plus Kalium and neutral lifecycle/collection contracts. Their former
assisted binding used the broad app-owned `ConversationCoreManualViewModelFactoryGroup`, which
would create an illegal feature-to-app edge if retained.

## Decision

Move the ViewModel and state package-preserving to `:features:conversation`. Introduce a
dedicated feature-owned assisted factory group and package-preserved Compose gateway. Remove the
two gateway overloads and binding imports from the broad app core graph; app session composition
installs the generated feature binding exactly once. Navigation 3 callers keep the same FQN and
typed arguments. Add an app assembly-ownership source test.

## Consequences

App production/tests become **979/284** and the conversation feature becomes **133/49**.
Strict app conversation production/tests become **156/53**. No resource, new Gradle edge,
navigation identity, profile, stability, KMP/iOS, or runtime behavior change occurs. Metro
ownership narrows from the broad app group to one feature capability group.

## Verification and rollback

Run the assembly-ownership test and `ConversationModuleBoundaryTest`, then compile feature/app
dev. Verify one session binding installation and no asset gateway in the app core graph. Revert
this commit to restore the former group/gateway.
