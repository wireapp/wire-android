# ADR 0118: Move guest-link action buttons to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `ce5ad447d`

## Context

The guest-link create, copy, share, and revoke button composables are conversation-details
presentation. They already depend only on neutral UI primitives; app ownership remained because
their four dedicated labels were app resources.

## Decision

Move `Buttons.kt` and `GuestLinkActionButtons.kt` package-preserving to
`:features:conversation`. Transfer the four guest-link action IDs and all 45 existing localized
definitions. Keep `EditGuestAccessScreen`, clipboard/share intents, ViewModels, Navigation 3, and
Metro assembly app-owned. Add exact source, qualifier, definition, and caller ownership tests.

## Consequences

App production/tests become **966/285** and strict app conversation production/tests become
**145/54**. The conversation feature becomes **145/57** and owns **992** string definitions.
Strict app conversation sources importing app `R` become **68**, with **342** distinct
resource-alias tokens. Packages, composable contracts, behavior, Metro, Navigation 3, profiles,
stability, Gradle edges, and KMP/iOS sources do not change.

## Verification and rollback

Run `GuestLinkActionButtonsOwnershipTest`, `ConversationModuleBoundaryTest`, and app dev
compilation. Revert this commit to restore app ownership.
