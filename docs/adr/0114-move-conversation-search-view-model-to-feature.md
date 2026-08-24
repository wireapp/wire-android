# ADR 0114: Move conversation search state to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `f11e0fb3d`

## Context

`SearchConversationMessagesViewModel` and its state use feature-owned navigation arguments and
search paging plus neutral UI/dispatcher contracts. Their assisted binding remained in a broad
app graph shared with unrelated folder routes.

## Decision

Move the ViewModel and state package-preserving to `:features:conversation`. Replace the obsolete
app search/folder factory group with a dedicated feature-owned search factory group and Compose
gateway. App session composition installs the generated binding exactly once, while Navigation 3
continues to call the same gateway/FQN. Add focused app assembly ownership coverage and keep the
existing behavior test.

## Consequences

App production/tests become **971/285**. Strict app conversation production/tests become
**149/54** and the conversation feature becomes **140/53**. Resources, Gradle edges, route
identity, public state/API, profiles, stability, and KMP/iOS sources do not change. The broad app
factory group no longer owns search or folder presentation.

## Verification and rollback

Run `SearchConversationMessagesViewModelTest`,
`SearchConversationMessagesViewModelAssemblyOwnershipSourceTest`, and app dev compilation. Revert
this commit to restore the former app group.
