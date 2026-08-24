# ADR 0093: Move message preview mapping and its closed resources to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `4dc4377c2afe229bacfba44b90352a7d13058b35`

## Context

`MessagePreviewContentMapper` projects Kalium preview content and unread-event
counts into the existing conversation-list `UILastMessageContent` contract. Its
42 app-owned resource IDs are consumed only by that mapper and its focused test:
30 string IDs with 286 definitions and 12 plural IDs with 111 definitions across
14 existing qualifier directories. The remaining two mapper string IDs are
already feature-owned and the unavailable-user label is neutral core-owned.

`UiTextResolver` is used independently outside the mapper, so it is neutral UI
infrastructure rather than conversation-specific implementation.

## Decision

Move `MessagePreviewContentMapper.kt` and its focused test from `:app` to
`:features:conversation`, preserving package, FQN, public extension functions,
constructor-free contract, mapping branches, and app consumer imports. Move
`UiTextResolver.kt` to `:core:ui-common` without changing its package, FQN, or
Android context behavior.

Move, rather than copy, every definition of the 42 preview-only IDs into the
matching existing conversation `strings.xml` qualifier. The message-presentation
ownership guard now covers the complete 59-ID / 608-definition closure, including
the existing 17-ID / 211-definition set. The mapper uses feature `R` for the
moved resources and retains the exact `"&nbsp;"` separator through its own private
constant; it no longer depends on `MarkdownConstants`.

The moved test replaces only app-local `TestMessage` and coroutine-extension
fixtures with a private minimal Kalium preview fixture. No Gradle edge, Metro
binding/group, Navigation 3, profile, stability, KMP, iOS, parser/renderer, host
adapter, or shared resource move accompanies this atom.

## Consequences

The post-move inventories are app production/tests **997/286**, conversation
feature production/tests **113/43**, and top-level app mapper production/tests
**3/3** versus conversation mapper production/tests **9/8**. The feature owns
497 string and 111 plural definitions for the guarded message-presentation
closure. `UiTextResolver` remains source-compatible for existing app and feature
consumers through its preserved FQN.

## Verification and rollback

Run the moved mapper test, resource-ownership and conversation-boundary tests,
relevant core UI-common tests, feature/core/app dev and fdroid Kotlin compilation,
and targeted app consumer tests. Verify the resource fingerprint, qualifier
coverage, no app/feature duplicate for the 42 moved IDs, move-first name status,
inventory counts, and `git diff --check`.

Rollback the three source/test moves, matching resource definitions, guards, and
documentation together if package/FQN behavior, resource resolution, or existing
app compilation changes. Stop rather than adding an architecture edge or widening
the resource closure.
