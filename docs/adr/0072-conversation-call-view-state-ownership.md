# ADR 0072: Conversation call view state ownership

**Status:** Accepted

**Date:** 2026-08-23

**Baseline:** `40d603300`

## Context

`ConversationCallViewState` models call information rendered by the conversation
screen. It is conversation-specific presentation state, not an application composition
concern or a shared calling capability, so neither `:app` nor `:core:calling` is its
correct owner.

## Decision

Move `ConversationCallViewState.kt` from `:app` to `:features:conversation` while
preserving its package, public FQN, byte content, behavior, callers, and imports. This
is a pure ownership move.

The declaration has no imports. The move adds no Gradle edge, resource, Metro binding,
navigation contract, or boundary-test allowlist. The dependency budget is therefore
zero new dependencies.

Stop the extraction if it requires an app import, a new module edge, a public API
change, moving call coordination into the feature, or moving conversation presentation
state into `:core:calling`. Those changes require a separate decision and reviewable
slice.

## Consequences

Conversation call presentation state is now owned by the conversation feature. The
package-preserving move keeps existing source and binary names stable while reducing
app-owned conversation implementation.

## Verification

Run `ConversationCallViewModelTest`, the conversation boundary test, feature
compilation, and sequential dev/fdroid app compilation with Java 21. Verify exact
source equality against the baseline, old-path absence, `git diff --check`, and 100%
rename detection.
