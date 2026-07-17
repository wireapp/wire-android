# Conversation Privacy Levels and Panic Mode

## Status

Proposed

## Context

We want to protect confidential conversations from accidental disclosure (meetings, screen sharing,
public environments, shared/unlocked devices) without enterprise complexity. The product requires:

- Per-conversation privacy levels — **Normal**, **Sensitive**, **Highly Sensitive** — that are **local
  to each user and NOT synchronized** with other participants or the backend.
- A global **Panic Mode** that temporarily escalates protection for all private conversations at once.

This introduces a new cross-cutting concern that touches the conversation list, the chat screen,
notifications, window security and authentication. It is a new architectural pattern, so it is
recorded here per AGENTS.md §5.

## Decision

### Local, unsynced storage (no Kalium changes)

Because the settings must not sync, they are **not** stored in the Kalium `Conversation` table (which
replicates to the backend). They live in a new app-side, per-user, encrypted `ConversationPrivacyStore`
(a `DataStore` file `conversation_privacy_<userId>`, modelled on `UserDataStore`), holding a single
JSON map keyed by conversation id, encrypted at rest with the existing `EncryptionManager`. Global
state (Panic Mode flags, Chat PIN, notification floor) lives in `GlobalDataStore`. **No Kalium/backend
changes are required.**

### One derived value: `EffectivePrivacyLevel`

A pure function `PrivacyResolver.resolve(baseLevel, panicActive)` collapses the per-conversation base
level and the global Panic Mode state into a single `EffectivePrivacyLevel`. Every consumer
(conversation list, chat screen, notifications, FLAG_SECURE, auto-lock) reads only this derived value,
so the behaviour matrix lives in one place and cannot drift between features. Panic Mode is a single
app-scoped `StateFlow` (`PanicModeManager`); flipping it re-emits effective levels everywhere.

### Reuse the existing lock stack

Highly-Sensitive authentication and auto-lock reuse `BiometricPromptUtils`, the `GlobalDataStore`
passcode/`sha256`/`EncryptionManager` pattern (for the Chat PIN), and the background-timeout approach
of `LockCodeTimeManager`. The new `SecureSessionManager` generalises "one global lock" to "N
per-conversation locks", and `SecureSessionCleaner` purges decrypted caches on lock.

### Notifications and window security

Notification redaction is applied at the single OS-facing choke point (`MessageNotificationManager`)
via `NotificationPrivacyMapper`, keyed by the notification's own userId (multi-account safe). The
secure window (`FLAG_SECURE`) reuses `WireActivity.HandleScreenshotCensoring`, OR-ing in Panic Mode.

### No new third-party dependencies

DataStore, BiometricPrompt, Coil and Metro are all already present. This ADR records a new *pattern*,
not a new library.

## Consequences

**Easier**
- Adding new privacy-aware behaviour: read `EffectivePrivacyLevel` from one repository.
- Reasoning about correctness: the entire base×panic matrix is one pure, exhaustively-tested function.
- Multi-account and no-session correctness for notifications (keyed by notification userId).

**More difficult / trade-offs**
- Privacy settings do not roam across a user's devices (by design — they are local).
- Decrypted plaintext in JVM `String`s cannot be force-zeroed; mitigated by withholding content while
  locked, purging Coil caches, and `FLAG_SECURE` (documented limitation, not a leak — the encrypted
  source remains in SQLCipher).
- `SecureSessionCleaner` clears Coil caches globally on lock (Coil keys are content hashes, not
  conversation-scoped); acceptable because lock events are infrequent.

## Implementation status (initial landing)

Implemented and wired: models + `PrivacyResolver`, `ConversationPrivacyStore`/`Repository`,
`PanicModeManager` + `GlobalDataStore` keys, Metro `PrivacyModule`, `SecureSessionManager` /
`SecureSessionCleaner` / `ConversationAuthenticator`, `NotificationPrivacyMapper` (wired into
`MessageNotificationManager`), and Panic-Mode-driven `FLAG_SECURE`.

Remaining (UI layer, follow-up): conversation-list redacted rendering, per-conversation Privacy
settings section, global Panic/Chat-PIN settings + entry points and indicator, chat-screen
blur/lock overlays + `SecureSessionManager` wiring into the conversation ViewModel, and Chat-PIN
create/enter screens with navigation. Design detail: `docs/design/conversation-privacy-and-panic-mode.md`.
