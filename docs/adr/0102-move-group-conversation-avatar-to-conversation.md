# ADR 0102: Move group conversation avatar to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `ee4fe6eda2cf2bca391ee0ed5c3b7c4210a56b6f`

## Context

`GroupConversationAvatar` renders feature-owned `ConversationAvatar.Group` data using neutral
core avatar primitives. Its app consumers already depend on the conversation facade.

## Decision

Move the source byte-for-byte to `:features:conversation`, preserving package, FQN, signature,
defaults, behavior, and all call sites. Register feature ownership and forbid the legacy path.

## Consequences

App production/tests become **987/283** and the conversation feature becomes **123/49**.
The app conversations-list production count becomes **27**; strict conversation counts do not
change. No resource, Gradle, Metro, Navigation 3, profile, stability, KMP/iOS, or runtime change.

## Verification and rollback

Run `ConversationModuleBoundaryTest`, compile feature and app dev, and verify a 100% rename.
Revert this single commit to restore the former owner.
