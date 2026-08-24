# ADR 0111: Move message expiration rendering to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `c04b0d380`

## Context

The expiration renderer depends on conversation-owned message state, timer state, icon metrics,
and four dedicated labels. Its only app-owned input is the localized unknown-user fallback, so
keeping the complete renderer in `:app` no longer represents a runtime boundary.

## Decision

Move `MessageExpirationItems.kt` package-preserving to `:features:conversation` with the four
dedicated resource IDs and their exact 31 existing definitions. Preserve the missing Spanish
translation and default fallback. Pass the already-resolved app `unknown_user_name` value through
an explicit `String` parameter at the two existing caller branches. Add focused ownership,
qualifier-coverage, and resource-fingerprint tests.

## Consequences

App production/tests become **976/284**. Strict app conversation production/tests become
**154/53** and the conversation feature becomes **135/51**. App conversation sources importing
app `R` become **73**, with **372** distinct resource-alias tokens. The feature owns **680** string
definitions. Public names, rendering and fallback behavior, Gradle edges, navigation, profiles,
stability, and KMP/iOS sources do not change.

## Verification and rollback

Run `MessageExpirationItemsResourceOwnershipTest`, the conversation boundary test, and app dev
compilation. Revert this commit to restore app ownership.
