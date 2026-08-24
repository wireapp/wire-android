# ADR 0103: Move participant previews to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `9b5c3a97b27c7ceafeb977a071648777bec0e70f`

## Context

Participant runtime renderers and preview state are already conversation-owned, but their two
preview-only source files remained in app because they used an app-internal preview annotation.

## Decision

Move both preview files to `:features:conversation`, preserving packages, FQNs, six preview
functions, and preview data. Replace only the app-internal annotation with the existing public
neutral `MultipleThemePreviews` annotation from UI-common.

## Consequences

App production/tests become **985/283** and the conversation feature becomes **125/49**.
Strict app conversation production/tests become **162/52**. Runtime bytecode, resources,
dependencies, Metro, Navigation 3, profiles, stability, KMP/iOS, and host behavior do not change.

## Verification and rollback

Run `ConversationModuleBoundaryTest`, compile feature/app dev in the batch checkpoint, and
verify that only the preview annotation imports/names differ from the moves. Revert this commit
to restore the app preview owner.
