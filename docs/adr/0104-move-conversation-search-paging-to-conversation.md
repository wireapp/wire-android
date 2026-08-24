# ADR 0104: Move conversation-search paging to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `de60e3292184844063f515fafa7b8a60b17c45e0`

## Context

`GetConversationMessagesFromSearchUseCase` pages search results for one conversation and maps
them through feature-owned message models/mapping. Its dependency closure is Paging, Kalium,
coroutines, dispatcher support, and conversation-owned presentation; it has no host dependency.

## Decision

Move the source byte-for-byte to `:features:conversation`, preserving package, FQN, injection
contract, paging behavior, and existing app test/callers. Register the exact dependency budget
and forbid the legacy app path.

## Consequences

App production/tests become **984/283** and the conversation feature becomes **126/49**.
Strict app conversation production/tests become **161/52**. No resource, Gradle, Metro,
Navigation 3, profile, stability, KMP/iOS, or host-runtime change occurs.

## Verification and rollback

Run the aggregate conversation boundary test and the existing focused app use-case test in the
batch checkpoint, then compile feature/app dev. Revert this single commit to restore ownership.
