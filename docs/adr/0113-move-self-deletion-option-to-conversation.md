# ADR 0113: Move the self-deletion option to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `7217f01ad`

## Context

`SelfDeletingMessageOption` is a details renderer built from conversation-owned option primitives.
Its only remaining app dependencies were two dedicated localized labels.

## Decision

Move the renderer package-preserving to `:features:conversation` with
`self_deleting_messages_option`, `self_deleting_messages_option_description`, and all 13 existing
localized definitions. Change only the resource namespace and add focused ownership/value-parity
tests.

## Consequences

App production/tests become **974/284**. Strict app conversation production/tests become
**152/53** and the conversation feature becomes **137/53**. App conversation sources importing
app `R` become **71**, with **348** distinct resource-alias tokens. The feature owns **911** string
definitions. Packages, public API, rendering, Gradle edges, navigation, profiles, stability, and
KMP/iOS sources do not change.

## Verification and rollback

Run `ConversationSelfDeletionOptionResourceOwnershipTest` and app dev compilation. Revert this
commit to restore app ownership.
