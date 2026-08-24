# ADR 0098: Move regular-message leading presentation to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `b5ef43631376a65cabb007c7711cb5b231033de4`

## Context

`RegularMessageItemLeading` renders the sender avatar at the leading edge of a regular
message. It depends only on conversation-owned message header/sender models and neutral
UI-common avatar, interaction, and resource contracts. Its two app-owned callers already
consume `:features:conversation`.

## Decision

Move the source byte-for-byte to `:features:conversation`, preserving its package, FQN,
public signature, behavior, imports, and both app call sites. Register the source in the
conversation boundary test and assert that the former app path stays absent.

## Consequences

App production/tests become **991/285** and the conversation feature becomes **119/45**.
Strict app conversation production becomes **167**. Resource, Gradle, Metro, Navigation 3,
profile, stability, KMP/iOS, and host-runtime ownership do not change.

## Verification and rollback

Run the focused conversation boundary test, compile the conversation feature and app dev
variant, and verify a 100% rename with `git diff --summary`. Roll back by reverting this
single commit; no data or navigation migration is involved.
