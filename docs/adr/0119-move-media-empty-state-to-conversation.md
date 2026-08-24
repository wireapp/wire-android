# ADR 0119: Move the media empty state to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `83548612c`

## Context

`EmptyMediaContentScreen` is pure conversation-media presentation. Its runtime contract already
accepts resolved text and depends only on neutral UI primitives; app ownership remained because
its previews referenced two app-owned empty-state labels.

## Decision

Move the renderer package-preserving to `:features:conversation`. Transfer
`label_conversation_files_empty`, `label_conversation_pictures_empty`, and all 14 existing
localized definitions. Use the neutral UI-common multi-theme preview annotation and the feature
resource namespace. Keep media routes, screens, ViewModels, Files/Cells integration, Android
side effects, Navigation 3, and Metro app-owned. Add exact source and resource ownership tests.

## Consequences

App production/tests become **965/285** and strict app conversation production/tests become
**144/54**. The conversation feature becomes **146/58** and owns **1006** string definitions.
Strict app conversation sources importing app `R` become **67**, with **342** distinct
resource-alias tokens. Packages, composable contracts, rendering, navigation identities,
profiles, Gradle edges, and KMP/iOS sources do not change.

## Verification and rollback

Run `EmptyMediaContentScreenOwnershipTest`, `ConversationModuleBoundaryTest`, and app dev
compilation. Revert this commit to restore app ownership.
