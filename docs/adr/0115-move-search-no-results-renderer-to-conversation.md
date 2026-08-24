# ADR 0115: Move the search no-results renderer to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `cd7c1297e`

## Context

The conversation search ViewModel, state, navigation arguments, and paging use case are
feature-owned, but the small no-results renderer remained in `:app`. Its only app dependency was
one dedicated localized label.

## Decision

Move `SearchConversationMessagesNoResultsScreen` package-preserving to
`:features:conversation`. Transfer `label_search_messages_no_results` with all seven existing
localized definitions, change only the resource namespace, and add focused ownership and exact
qualifier/value fingerprint coverage. Add the complete search ViewModel/state/graph and renderer
set to the shared conversation boundary ownership inventory.

## Consequences

App production/tests become **970/285**. Strict app conversation production/tests become
**148/54** and the conversation feature becomes **141/54**. App conversation sources importing
app `R` become **70**, with **347** distinct resource-alias tokens. The feature owns **918** string
definitions. Packages, public API, rendering, Metro assembly, Navigation 3 identities, profiles,
stability, Gradle edges, and KMP/iOS sources do not change.

## Verification and rollback

Run `SearchConversationMessagesNoResultsResourceOwnershipTest`, `ConversationModuleBoundaryTest`,
and app dev compilation. Revert this commit to restore app ownership.
