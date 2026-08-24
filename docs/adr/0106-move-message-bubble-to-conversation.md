# ADR 0106: Move message-bubble presentation to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `c663ed8ab8e0c790fb859755e94fdd05a0c8cefb`

## Context

`MessageBubbleItem` renders conversation-owned regular-message models using neutral theme and
UI-common primitives plus the already feature-owned combined-click interceptor. It has no app
resource, host, navigation, or DI dependency.

## Decision

Move the source byte-for-byte to `:features:conversation`, preserving package, FQN, public
signature, layout, interaction behavior, defaults, and app callers. Register source ownership.

## Consequences

App production/tests become **982/283** and the conversation feature becomes **128/49**.
Strict app conversation production/tests become **159/52**. Resources, dependencies, Metro,
Navigation 3, profiles, stability, KMP/iOS, and host runtime do not change.

## Verification and rollback

Run `ConversationModuleBoundaryTest`, compile feature/app dev, and verify a 100% rename. Revert
this single commit to restore app ownership.
