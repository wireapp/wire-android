# ADR 0100: Move self-deletion timer state to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `fd2375634fe2e89ffdc20db2329895e3a6136ba8`

## Context

`MessageExpiration.kt` owns the self-deletion countdown state, formatting, lifecycle refresh,
and timer callback used by conversation message rendering. It depends on conversation-owned
message models, neutral current-time support, Compose/lifecycle APIs, and ten timer plural IDs.
The 30-case unit test is the behavior contract. The app renderer temporarily remains a caller
of the shared `seconds_left` plural until the expiration renderer moves.

## Decision

Move the timer source and all 30 existing tests to `:features:conversation`, preserving their
packages, FQNs, public surface, and behavior. Move exactly ten plural IDs and 65 definitions
across default, German, Spanish, Estonian, Hungarian, Italian, Portuguese, Russian, Sinhala,
and Swedish qualifiers. The app expiration renderer references the feature-owned plural during
the transition. Add only the direct lifecycle Compose dependency required by
`LocalLifecycleOwner` and enforce source/resource ownership with focused tests.

## Consequences

App production/tests become **989/284** and the conversation feature becomes **121/48**.
Strict app conversation production/tests become **165/53**. Files importing app `R` in the
strict conversation tree fall to **75**, with **385** distinct resource-alias `R.type.name` tokens. The
feature keeps its 615 string definitions and additionally owns all **65** timer plural
definitions. No Metro, Navigation 3, profile, stability, KMP/iOS, or host-runtime change occurs.

## Verification and rollback

Run `SelfDeletionTimerTest`, `ConversationSelfDeletionTimerResourceOwnershipTest`, and
`ConversationModuleBoundaryTest`, then compile the feature and app dev variant. Validate that
all ten plural IDs are absent from app and present in the exact qualifier set. Revert this
single commit to restore source, tests, resources, and the prior app resource reference.
