# ADR 0112: Move group conversation options to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `dfa7c5b0f`

## Context

`GroupConversationOptions` renders feature-owned details state but remained in `:app`. Its item
shell and switch row are also used by app-hosted settings flows, while its disable-confirmation
dialog consumes shared app labels. Moving the screen as one opaque file would therefore create an
illegal feature-to-app resource dependency.

## Decision

Move `GroupConversationOptions` package-preserving to `:features:conversation` with its 26
dedicated resource IDs and all 218 existing definitions. Move the reusable
`GroupConversationOptionsItem`, `ArrowType`, and `GroupOptionWithSwitch` presentation seams to
`:core:ui-common`. Keep `DisableConfirmationDialog` app-owned in a dedicated file. Update the nine
remaining app resource callers to conversation `R` and use the public UI-common preview
annotation. Add focused ownership and qualifier-coverage tests.

## Consequences

App production/tests become **975/284**. Strict app conversation production/tests become
**153/53** and the conversation feature becomes **136/52**. App conversation sources importing
app `R` become **72**, with **350** distinct resource-alias tokens. The feature owns **898** string
definitions. UI common gains two package-preserved production sources. Public names, option
behavior, Gradle edges, navigation, profiles, stability, and KMP/iOS sources do not change.

## Verification and rollback

Run `ConversationGroupOptionsPresentationOwnershipTest`, compile UI common, and compile app dev.
Revert this commit to restore the previous ownership.
