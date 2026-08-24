# ADR 0110: Move message status presentation to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `73a966f11`

## Context

`MessageStatusIndicator` renders conversation-owned delivery state but remained in `:app` with
five app drawables and three accessibility labels. It has no host-runtime responsibility and is a
leaf of the conversation timeline.

## Decision

Move the renderer package-preserving to `:features:conversation`. Move its three dedicated
drawables and all 34 existing localized accessibility-string definitions to the same owner. Move
the two drawables also consumed by app UI to `:core:ui-common` and update callers to common `R`. Use
the public UI-common preview annotation and add focused ownership, localization, and drawable
fingerprint tests.

## Consequences

Strict app conversation production/tests become **155/53**; the conversation feature becomes
**134/50**. App conversation sources importing app `R` become **74**, with **377** distinct
resource-alias tokens. The feature owns **649** string definitions. Public names, rendering,
localized values, Gradle edges, Navigation 3, profiles, stability, and KMP/iOS sources do not
change.

## Verification and rollback

Run `ConversationMessageStatusIndicatorResourceOwnershipTest`, the conversation boundary test,
and app dev compilation. Revert this commit to restore app ownership.
