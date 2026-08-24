# ADR 0120: Move the muted-conversation badge to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `7d8ef7d5c`

## Context

`MutedConversationBadge` is conversation-list presentation. Its only source dependency is neutral
UI presentation, but its accessibility label and mute icon were app resources. The icon also has
one app-owned conversation bottom-sheet consumer.

## Decision

Move the badge package-preserving to `:features:conversation`. Transfer
`content_description_muted_conversation` with all 17 localized definitions and the byte-identical
`ic_mute` drawable. Keep `ConversationMainSheetContent` app-owned and change only that caller's
drawable namespace to the feature resource. Use the neutral UI-common preview annotation and add
exact source, qualifier, value, consumer, and drawable-hash ownership tests.

## Consequences

App production/tests become **964/285**; strict app conversation production/tests remain
**144/54**, and app conversation-list production becomes **26**. The conversation feature becomes
**147/59** and owns **1023** string definitions plus `ic_mute`. Strict app conversation sources
importing app `R` remain **67**, with **341** distinct resource-alias tokens. Packages, composable
contracts, rendering, Navigation 3, Metro, profiles, stability, Gradle edges, and KMP/iOS sources
do not change.

## Verification and rollback

Run `MutedConversationBadgeResourceOwnershipTest`, `ConversationModuleBoundaryTest`, and app dev
compilation. Revert this commit to restore app ownership.
