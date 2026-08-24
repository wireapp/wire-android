# ADR 0105: Move conversation-asset paging to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `7290998d412aa70bf3e3365873b8153308b9b65b`

## Context

`GetAssetMessagesFromConversationUseCase` pages conversation assets, maps feature-owned message
models, and inserts month/year labels represented by its local `UIPagingItem` hierarchy. Its
dependency closure is already present in the conversation facade and contains no host APIs.

## Decision

Move the source and `UIPagingItem` byte-for-byte to `:features:conversation`, preserving package,
FQNs, injection contract, paging/separator behavior, and callers. Register the exact dependency
budget and forbid the legacy app path.

## Consequences

App production/tests become **983/283** and the conversation feature becomes **127/49**.
Strict app conversation production/tests become **160/52**. No resource, Gradle, Metro,
Navigation 3, profile, stability, KMP/iOS, or host-runtime change occurs.

## Verification and rollback

Run `ConversationModuleBoundaryTest` and compile feature/app dev in the shared batch checkpoint.
Revert this single commit to restore the prior source owner.
