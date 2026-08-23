# ADR 0071: Conversation screen dialog type ownership

**Status:** Accepted

**Date:** 2026-08-23

**Baseline:** `1330088c4`

## Context

`ConversationScreenDialogType` models the dialog state presented by the conversation
screen. It is conversation-specific presentation state, not an application composition
concern or a neutral UI primitive, so leaving it in `:app` keeps feature implementation
owned by the host.

## Decision

Move `ConversationScreenDialogType.kt` from `:app` to `:features:conversation` while
preserving its package, public FQN, enum entries, behavior, callers, and imports. This
is a pure ownership move.

The declaration has no dependencies. The move adds no Gradle edge, resource, Metro
binding, navigation contract, or boundary-test allowlist.

Stop the extraction if it requires an app import, a new module edge, a public API
change, or moving conversation-screen implementation beyond this state type. Those
changes require a separate decision and reviewable slice.

## Consequences

Conversation-screen dialog state is now owned by the conversation feature. The
package-preserving move keeps existing source and binary names stable while reducing
app-owned conversation implementation.

## Verification

Run the conversation boundary test, feature compilation, and sequential dev/fdroid
app compilation with Java 21. Verify exact source equality against the baseline,
old-path absence, `git diff --check`, and 100% rename detection.
