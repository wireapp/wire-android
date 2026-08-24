# ADR 0099: Move offline-message paging presentation to the conversation facade

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `b7c4068d45a499ca89f9d8e3905fc3e574e098d1`

## Context

The offline-message separator is a conversation timeline transformation. Its implementation
depends only on Paging, conversation-owned UI message models, neutral UI contracts, Kalium's
`ConversationId`, and the clock. The app-owned `ConversationMessagesViewModel` is its only
production caller while the remaining timeline closure is migrated.

## Decision

Move `OfflineMessageIndicator.kt` to `:features:conversation` with its package and behavior
preserved. Widen only `withOfflineIndicator` from module-internal to public so the temporary
app caller can keep using the same FQN; keep the message-construction helper internal. Move the
five focused paging cases from the broad app ViewModel test into a dedicated feature test,
including the exact two-block separator IDs.

## Consequences

App production/tests become **990/285** and the conversation feature becomes **120/46**.
Strict app conversation production/tests become **166/53**. No resources, Gradle edges, Metro,
Navigation 3, profile, stability, KMP/iOS, or host runtime change. The one widened function is a
temporary facade surface and can become internal again when the app timeline caller moves.

## Verification and rollback

Run `OfflineMessageIndicatorTest`, `ConversationModuleBoundaryTest`, the remaining app
`ConversationMessagesViewModelTest`, and compile the feature plus app dev variant. Revert this
single commit to restore app ownership; no persisted state or navigation identity changes.
