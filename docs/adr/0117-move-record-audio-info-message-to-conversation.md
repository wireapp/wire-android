# ADR 0117: Move record-audio information messages to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `fbc1961ab`

## Context

`RecordAudioInfoMessageType` is composer presentation used only inside the conversation flow. Its
sealed snackbar contract already depends on feature/core-owned types; app ownership remained
because two dedicated failure labels were app resources.

## Decision

Move the message type package-preserving to `:features:conversation`. Transfer
`record_audio_unable_due_to_ongoing_call`, `record_audio_unable_due_to_error`, and all 14 existing
localized definitions. Keep `EnabledMessageComposer` app-owned and change its direct toast to the
feature resource namespace. Add exact qualifier/definition fingerprint and caller ownership tests.

## Consequences

App production/tests become **968/285** and app message-composer production becomes **39**. Strict
app conversation production/tests remain **147/54**. The conversation feature becomes
**143/56** and owns **947** string definitions. Strict app conversation sources importing app `R`
remain **69**, with **345** distinct resource-alias tokens. Packages, snackbar behavior, public
API, Metro, Navigation 3, profiles, stability, Gradle edges, and KMP/iOS sources do not change.

## Verification and rollback

Run `RecordAudioInfoMessageResourceOwnershipTest`, `ConversationModuleBoundaryTest`, and app dev
compilation. Revert this commit to restore app ownership.
