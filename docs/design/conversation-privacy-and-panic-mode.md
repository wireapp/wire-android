# Conversation Privacy Levels + Panic Mode — Production Design

> Status: **Proposal / design** · Scope: Android (this repo) with a parallel iOS section · Calls deliberately deferred to **v2**.
>
> This document is grounded in the **actual** Wire Android architecture (Metro DI, `UserDataStore`/`GlobalDataStore`, `WireNotificationManager`/`MessageNotificationManager`, `CurrentScreenManager`, `LockCodeTimeManager`, `BiometricPromptUtils`, `ConversationItem`/`ConversationMapper`). Class and file references use `path:line` form.

---

## 0. Design ground rules (read first)

Three decisions shape everything below.

1. **Privacy settings are LOCAL, never synced.** The spec requires per-conversation settings that are *not* shared with other participants. The Kalium `Conversation` table (mute, archive, favorites) is **replicated to the backend** — that is exactly how those settings sync across a user's devices. Therefore privacy levels **must not** be stored as a Kalium DB column or any Kalium-synced field. They live in an **app-side, per-user store** (`ConversationPrivacyStore`, modeled on `UserDataStore` at `app/src/main/kotlin/com/wire/android/datastore/UserDataStore.kt`). This also means the design lives entirely in the `app` module + `core` and touches **zero** Kalium domain logic, so no submodule changes and no backend dependency.

2. **Reuse the existing lock stack; do not build a parallel one.** Highly-Sensitive auth/auto-lock is the same conceptual machine as the existing app-lock:
   - `LockCodeTimeManager` (`app/.../ui/home/appLock/LockCodeTimeManager.kt`) — background-timeout → locked.
   - `BiometricPromptUtils` (`app/.../biometric/BiometricPromptUtils.kt`) — biometric + device-credential.
   - `GlobalDataStore` passcode storage with `EncryptionManager` + `sha256` — Chat PIN reuses this exact pattern.
   We generalize these from "one global lock" to "global lock + N per-conversation locks."

3. **One concept rules them all: the *effective* privacy level.** A conversation has a *base* level (user setting). Panic Mode is a global modifier that *escalates* behavior at runtime. Every consumer (list, chat screen, notifications, window flags, auto-lock) reads a single derived value — the **effective level** — never the raw base level + a separate panic check. This keeps the behavior matrix in one place and impossible to get inconsistent.

---

## 1. Database / persistence schema

No SQL migration in Kalium. Two app-side stores.

### 1.1 Per-conversation privacy (per-user, local)

A dedicated Preferences DataStore file per user, holding **one serialized map** keyed by conversation id. A single-key map (rather than one preference key per conversation) lets the conversation list observe *all* levels in one cheap flow — essential because every visible row needs its level to render.

File name: `conversation_privacy_<userId>` (mirrors `UserDataStore`'s `user_data_<userId>` convention at `UserDataStore.kt:37`).

```jsonc
// Value stored under key "privacy_map" (a JSON string):
{
  "uuid@domain": { "level": "SENSITIVE",         "autoLock": "THIRTY_SECONDS" },
  "uuid2@dom":  { "level": "HIGHLY_SENSITIVE",    "autoLock": "IMMEDIATELY"  }
  // conversations absent from the map are implicitly NORMAL (the default)
}
```

- **At rest**: the JSON blob is encrypted with the existing `EncryptionManager` (same primitive that protects the app-lock passcode, `GlobalDataStore.kt:149`) before being written, so an attacker with raw DataStore file access cannot enumerate which conversations are highly sensitive. (Metadata-confidentiality; the *content* is already in the SQLCipher Kalium DB.)
- **Default = NORMAL** means existing conversations need no backfill.
- **Why not Room?** Adding Room is a new dependency (ADR-gated) and the codebase standardizes on DataStore for app-local settings. The map is small (bounded by conversation count, and only non-NORMAL entries are stored).

### 1.2 Global privacy + Panic Mode (app-wide)

Stored in `GlobalDataStore` (`app/.../datastore/GlobalDataStore.kt`, `@SingleIn(AppScope::class)`). New keys:

| Key | Type | Meaning |
|---|---|---|
| `panic_mode_active` | Boolean | Is panic mode on right now |
| `panic_mode_expires_at` | Long? (epoch ms) | Auto-disable deadline; `null` ⇒ "Until disabled" |
| `panic_default_duration` | String enum | User's default duration choice |
| `chat_pin_hash` | String (encrypted) | Chat PIN, `sha256` then `EncryptionManager.encrypt` (reuses app-lock pattern) |
| `notif_privacy_default` | String enum | Global notification-privacy floor (optional override) |

Persisting `panic_mode_expires_at` (not a live countdown) makes panic mode **survive process death**: on cold start we recompute "still active?" from the wall clock, so killing the app cannot be used to escape panic mode early, and an expired timer self-heals.

---

## 2. Data models

Placed in `app/src/main/kotlin/com/wire/android/feature/privacy/` (pure Kotlin, no Android deps in the model file so it is trivially unit-testable). If later shared, this package lifts cleanly into a `features/privacy` module — it has no `app`-only dependencies by design.

```kotlin
package com.wire.android.feature.privacy

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable

/** Base, user-chosen privacy level for a conversation. Ordinal order == strength order. */
@Serializable
enum class ConversationPrivacyLevel { NORMAL, SENSITIVE, HIGHLY_SENSITIVE }

/** Per-conversation auto-lock / auto-hide delay. */
@Serializable
enum class AutoLockTimeout(val delay: Duration) {
    IMMEDIATELY(Duration.ZERO),
    THIRTY_SECONDS(30.seconds),
    ONE_MINUTE(1.minutes),
    FIVE_MINUTES(5.minutes),
}

@Serializable
data class ConversationPrivacySettings(
    val level: ConversationPrivacyLevel = ConversationPrivacyLevel.NORMAL,
    val autoLock: AutoLockTimeout = AutoLockTimeout.IMMEDIATELY,
)

@Serializable
enum class PanicDuration {
    FIFTEEN_MINUTES, ONE_HOUR, UNTIL_DISABLED;
}

/** Runtime panic state. `expiresAt == null` while inactive OR "until disabled". */
sealed interface PanicModeState {
    data object Inactive : PanicModeState
    data class Active(val expiresAtEpochMs: Long?) : PanicModeState // null => until disabled
}

/**
 * The single value every consumer reads. Combines base level + panic into a behavior class.
 * NORMAL is never escalated by panic (spec). Panic upgrades the *behavior* of the two private tiers.
 */
enum class EffectivePrivacyLevel {
    NORMAL,                 // full content everywhere
    SENSITIVE,             // hide previews, blur-on-leave, reveal by tap (no auth)
    SENSITIVE_PANIC,       // SENSITIVE + immediate blur on focus loss + secure window + hidden notif content
    HIGHLY_SENSITIVE,      // auth to view, auto-lock, secure session cleanup
    HIGHLY_SENSITIVE_PANIC; // HIGHLY_SENSITIVE forced locked NOW + all metadata hidden

    val requiresAuth: Boolean get() = this == HIGHLY_SENSITIVE || this == HIGHLY_SENSITIVE_PANIC
    val hidesPreviewInList: Boolean get() = this != NORMAL
    val hidesIdentityInList: Boolean get() = this == HIGHLY_SENSITIVE || this == HIGHLY_SENSITIVE_PANIC
    val needsSecureWindow: Boolean get() = this != NORMAL // FLAG_SECURE while open
}
```

### 2.1 The resolver (the heart of the feature)

```kotlin
object PrivacyResolver {
    fun resolve(base: ConversationPrivacyLevel, panicActive: Boolean): EffectivePrivacyLevel =
        when (base) {
            ConversationPrivacyLevel.NORMAL -> EffectivePrivacyLevel.NORMAL
            ConversationPrivacyLevel.SENSITIVE ->
                if (panicActive) EffectivePrivacyLevel.SENSITIVE_PANIC else EffectivePrivacyLevel.SENSITIVE
            ConversationPrivacyLevel.HIGHLY_SENSITIVE ->
                if (panicActive) EffectivePrivacyLevel.HIGHLY_SENSITIVE_PANIC else EffectivePrivacyLevel.HIGHLY_SENSITIVE
        }
}
```

This pure function is the most-tested unit in the feature — every behavior decision in §6–§11 funnels through it.

---

## 3. Privacy-level architecture (layers & data flow)

```
                ┌───────────────────────────────────────────────┐
                │  ConversationPrivacyStore (per-user DataStore)  │  ← base levels (local)
                │  GlobalDataStore (panic flags, chat pin)        │  ← global (local)
                └───────────────────────┬───────────────────────┘
                                         │ Flow<Map<ConvId,Settings>>, Flow<PanicModeState>
                ┌───────────────────────▼───────────────────────┐
                │  PanicModeManager  (@SingleIn AppScope)         │  StateFlow<PanicModeState>, timer
                │  ConversationPrivacyRepository                  │  observe/update base settings
                └───────────────────────┬───────────────────────┘
                                         │ combine → effective level
        ┌────────────────┬──────────────┼───────────────┬──────────────────┐
        ▼                ▼              ▼               ▼                  ▼
 ConversationList  Chat screen     Notification    Secure window      Auto-lock
   ViewModel       SecureSession   PrivacyMapper   (FLAG_SECURE)      Controller
   (hide preview)  Manager(lock)   (redact)        WireActivity       (timeout→lock)
```

### 3.1 Repository

```kotlin
interface ConversationPrivacyRepository {
    /** All non-NORMAL conversations (NORMAL is implicit). Cheap; for the list. */
    fun observeAll(): Flow<Map<ConversationId, ConversationPrivacySettings>>
    fun observe(id: ConversationId): Flow<ConversationPrivacySettings>
    /** Base level combined with live panic state. The value consumers actually want. */
    fun observeEffective(id: ConversationId): Flow<EffectivePrivacyLevel>
    suspend fun setLevel(id: ConversationId, level: ConversationPrivacyLevel)
    suspend fun setAutoLock(id: ConversationId, timeout: AutoLockTimeout)
    suspend fun get(id: ConversationId): ConversationPrivacySettings // one-shot for notifications
}
```

```kotlin
@SingleIn(AppScope::class)
@Inject
class ConversationPrivacyRepositoryImpl(
    private val store: ConversationPrivacyStore,        // wraps the per-user DataStore (§1.1)
    private val panicModeManager: PanicModeManager,
) : ConversationPrivacyRepository {

    override fun observeAll() = store.observeMap()

    override fun observe(id: ConversationId) =
        store.observeMap().map { it[id] ?: ConversationPrivacySettings() }.distinctUntilChanged()

    override fun observeEffective(id: ConversationId): Flow<EffectivePrivacyLevel> =
        combine(observe(id), panicModeManager.state) { settings, panic ->
            PrivacyResolver.resolve(settings.level, panic is PanicModeState.Active)
        }.distinctUntilChanged()

    override suspend fun get(id: ConversationId) =
        store.observeMap().first()[id] ?: ConversationPrivacySettings()

    override suspend fun setLevel(id: ConversationId, level: ConversationPrivacyLevel) =
        store.update(id) { it.copy(level = level) }

    override suspend fun setAutoLock(id: ConversationId, timeout: AutoLockTimeout) =
        store.update(id) { it.copy(autoLock = timeout) }
}
```

`ConversationPrivacyStore` is the thin DataStore wrapper (encrypt/decrypt JSON map via `EncryptionManager`, `observeMap(): Flow<Map<...>>`, `update(id) { copy }`). It is per-user, so it's provided in the session scope keyed by `@CurrentAccount UserId`, exactly like `UserDataStoreProvider.getOrCreate(userId)`.

---

## 4. Conversation state machine (chat screen access)

A conversation that is open on screen runs through a state machine. SENSITIVE and HIGHLY_SENSITIVE share one machine; the `requiresAuth` flag of the effective level selects the path.

```
                       open(effLevel)
                            │
          ┌─────────────────┴───────────────────┐
   effLevel.requiresAuth == false        requiresAuth == true
          │                                       │
          ▼                                       ▼
       ┌──────┐  background / lock /           ┌────────┐
       │VISIBLE│──── timeout / panic ─────────▶│ HIDDEN │   (blur overlay, no auth)
       └──────┘◀──────── tap to reveal ────────└────────┘
          ▲                                          (SENSITIVE family)
          └ enter foreground (still VISIBLE; sensitive only blurs, never locks)

   ──────────────────────── HIGHLY_SENSITIVE family ───────────────────────
        ┌────────┐  auth requested   ┌──────────────┐  success   ┌──────────┐
        │ LOCKED │─────────────────▶ │AUTHENTICATING│──────────▶ │ UNLOCKED │
        └────────┘ ◀── fail/cancel ──└──────────────┘            └────┬─────┘
            ▲                                                         │
            │ background │ device-lock │ inactivity-timeout │ manual  │
            │ lock │ PANIC activated  ◀─────────────────────────────-─┘
            │
        (on entering LOCKED: SecureSessionCleaner.purge(convId))
```

Unified state type:

```kotlin
sealed interface ConversationAccessState {
    data object Visible : ConversationAccessState            // normal, or sensitive revealed, or unlocked
    data object Concealed : ConversationAccessState           // sensitive: blurred, tap to reveal
    data object Locked : ConversationAccessState              // highly-sensitive: needs auth
    data object Authenticating : ConversationAccessState
}
```

Transition table (driver = `SecureSessionManager`, §6):

| From | Event | Effective family | To | Side effect |
|---|---|---|---|---|
| any | `open` | NORMAL | Visible | — |
| any | `open` | SENSITIVE\* | Visible | start inactivity timer |
| any | `open` | HIGHLY\* | Locked | request auth (see §7) |
| Visible | `background`/`deviceLock`/`timeout` | SENSITIVE\* | Concealed | — |
| Visible | `panicActivated` | SENSITIVE\* | Concealed | (immediate) |
| Concealed | `tapReveal` | SENSITIVE\* | Visible | restart timer |
| Locked | `authStart` | HIGHLY\* | Authenticating | show BiometricPrompt / PIN |
| Authenticating | `authSuccess` | HIGHLY\* | Unlocked→Visible | start inactivity timer |
| Authenticating | `authFail`/`cancel` | HIGHLY\* | Locked | — |
| Visible | `background`/`deviceLock`/`timeout`/`manualLock`/`panicActivated` | HIGHLY\* | Locked | **`SecureSessionCleaner.purge`** |

Note: a conversation already open when its *effective* level rises (panic flips on) re-evaluates immediately: SENSITIVE→Concealed, HIGHLY_SENSITIVE→Locked+purge. That is `panicActivated` arriving as a flow emission, not a user action.

---

## 5. Panic Mode architecture

```kotlin
@SingleIn(AppScope::class)
@Inject
class PanicModeManager(
    @ApplicationScope private val scope: CoroutineScope,
    private val globalDataStore: GlobalDataStore,
    private val currentTime: CurrentTimeProvider, // existing abstraction used by LockCodeTimeManager
) {
    private val _state = MutableStateFlow<PanicModeState>(PanicModeState.Inactive)
    val state: StateFlow<PanicModeState> = _state.asStateFlow()
    private var expiryJob: Job? = null

    init {
        // Recompute from persisted flags on cold start (survives process death).
        scope.launch {
            val active = globalDataStore.isPanicActive().first()
            val expiresAt = globalDataStore.panicExpiresAt().first()
            if (active) activateInternal(expiresAt, persist = false)
        }
    }

    suspend fun activate(duration: PanicDuration) {
        val expiresAt = when (duration) {
            PanicDuration.FIFTEEN_MINUTES -> currentTime().toEpochMilliseconds() + 15.minutes.inWholeMilliseconds
            PanicDuration.ONE_HOUR       -> currentTime().toEpochMilliseconds() + 1.hours.inWholeMilliseconds
            PanicDuration.UNTIL_DISABLED -> null
        }
        activateInternal(expiresAt, persist = true)
    }

    private fun activateInternal(expiresAt: Long?, persist: Boolean) {
        expiryJob?.cancel()
        if (persist) scope.launch { globalDataStore.setPanic(active = true, expiresAt = expiresAt) }

        val now = runBlockingNow()
        if (expiresAt != null && expiresAt <= now) { scope.launch { deactivate() }; return }

        _state.value = PanicModeState.Active(expiresAt)
        if (expiresAt != null) {
            expiryJob = scope.launch {
                delay(expiresAt - now)
                deactivate()
            }
        }
    }

    suspend fun deactivate() {
        expiryJob?.cancel()
        _state.value = PanicModeState.Inactive
        globalDataStore.setPanic(active = false, expiresAt = null)
    }
}
```

Why a single app-scoped `StateFlow`: every consumer (`observeEffective`, notification mapper, window-flag controller, auto-lock) subscribes to the same source. Activation is **idempotent and instant** — flipping `_state` causes every open highly-sensitive conversation's `observeEffective` to emit `HIGHLY_SENSITIVE_PANIC`, which the `SecureSessionManager` turns into a lock+purge with no extra plumbing. That is the "protect everything immediately" UX requirement, realized as one flow emission.

Activation entry points (spec): Settings, profile menu, quick-actions menu, conversation-list shortcut — all call `panicModeManager.activate(defaultDuration)` (or open a duration picker). A persistent indicator (`🛡 Panic Mode Active`) is a top banner driven by `state`.

---

## 6. Secure-session / auto-lock controller

Generalizes `LockCodeTimeManager` from one global lock to per-conversation locks, **reusing its exact background-timeout logic** (`LockCodeTimeManager.kt:73-115`).

```kotlin
@SingleIn(AppScope::class)
@Inject
class SecureSessionManager(
    @ApplicationScope private val scope: CoroutineScope,
    private val currentScreenManager: CurrentScreenManager,   // isAppVisibleFlow()
    private val screenStateObserver: ScreenStateObserver,     // device-lock (screen off)
    private val privacyRepository: ConversationPrivacyRepository,
    private val cleaner: SecureSessionCleaner,
    private val currentTime: CurrentTimeProvider,
) {
    // Conversations the user has authenticated this session.
    private val unlocked = MutableStateFlow<Set<ConversationId>>(emptySet())
    // Per-conversation inactivity timer reset signal.
    private val activityPing = MutableSharedFlow<ConversationId>(extraBufferCapacity = 8)

    /** Drives the chat screen. */
    fun observeAccessState(id: ConversationId): Flow<ConversationAccessState> =
        combine(
            privacyRepository.observeEffective(id),
            unlocked.map { id in it },
            currentScreenManager.isAppVisibleFlow(),
            screenStateObserver.screenOnFlow(),
        ) { eff, isUnlocked, appVisible, screenOn -> Eval(eff, isUnlocked, appVisible, screenOn) }
            .flatMapLatest { e -> resolveAccess(id, e) }
            .distinctUntilChanged()

    private fun resolveAccess(id: ConversationId, e: Eval): Flow<ConversationAccessState> = flow {
        when {
            e.eff == EffectivePrivacyLevel.NORMAL -> emit(Visible)

            !e.eff.requiresAuth -> { // SENSITIVE family: conceal vs visible, never auth
                val immediatePanic = e.eff == EffectivePrivacyLevel.SENSITIVE_PANIC
                if (!e.appVisible || !e.screenOn) { emit(Concealed); return@flow }
                if (immediatePanic) { emit(Concealed); return@flow } // panic = blur immediately on any focus jitter
                emit(Visible)
                // inactivity timeout → conceal
                val timeout = privacyRepository.get(id).autoLock.delay
                if (timeout > Duration.ZERO) {
                    awaitInactivity(id, timeout); emit(Concealed)
                }
            }

            else -> { // HIGHLY_SENSITIVE family
                val forceLock = e.eff == EffectivePrivacyLevel.HIGHLY_SENSITIVE_PANIC
                if (forceLock || !e.isUnlocked) { lock(id); emit(Locked); return@flow }
                if (!e.appVisible || !e.screenOn) {
                    // background/screen-off → start timeout (reuse LockCodeTimeManager pattern)
                    val timeout = privacyRepository.get(id).autoLock.delay
                    delay(timeout.inWholeMilliseconds)
                    lock(id); emit(Locked); return@flow
                }
                emit(Visible)
                val timeout = privacyRepository.get(id).autoLock.delay
                if (timeout > Duration.ZERO) { awaitInactivity(id, timeout); lock(id); emit(Locked) }
            }
        }
    }

    fun markUnlocked(id: ConversationId) { unlocked.update { it + id } }
    fun manualLock(id: ConversationId) { lock(id) }
    fun userActivity(id: ConversationId) { activityPing.tryEmit(id) }

    private fun lock(id: ConversationId) {
        unlocked.update { it - id }
        scope.launch { cleaner.purge(id) }      // §11 secure cleanup
    }

    private suspend fun awaitInactivity(id: ConversationId, timeout: Duration) {
        while (true) {
            val resumed = withTimeoutOrNull(timeout.inWholeMilliseconds) {
                activityPing.first { it == id }; true
            }
            if (resumed == null) return // timed out → caller locks/conceals
        }
    }
}
```

Triggers covered exactly per spec:
- **App backgrounds** → `currentScreenManager.isAppVisibleFlow()` flips false (the same source `LockCodeTimeManager` uses).
- **Device locks** → `ScreenStateObserver` screen-off (already combined into `isAppVisibleFlow`'s upstream; we also read it directly so screen-off locks even with the app technically foregrounded).
- **Inactivity timeout** → per-conversation `awaitInactivity` reset by `userActivity()` calls from the chat screen (touch/scroll/typing).
- **Manual lock** → `manualLock(id)` (a lock button in the chat top bar).
- **Panic** → effective level becomes `*_PANIC`, re-emits, force-locks.

---

## 7. Authentication flow

Reuses `BiometricPromptUtils.showBiometricPrompt()` and the app-lock passcode storage. Adds a **Chat PIN** as a fallback when no biometric/device credential exists.

```kotlin
/** Abstracts "prove it's you" for highly-sensitive conversations. */
@Inject
class ConversationAuthenticator(
    private val globalDataStore: GlobalDataStore, // chat pin hash, reuses EncryptionManager
) {
    sealed interface Result { data object Success: Result; data object Failed: Result; data object Cancelled: Result }

    /** Tries biometric/device-credential first; UI falls back to Chat PIN screen on negative button. */
    fun authenticate(activity: AppCompatActivity, onResult: (Result) -> Unit) {
        val canBio = BiometricManager.from(activity)
            .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        if (canBio) {
            activity.showBiometricPrompt(/* allow DEVICE_CREDENTIAL */) { /* map to Result */ }
        } else {
            onResult(Result.Failed) // caller routes to Chat PIN screen (or PIN-creation if none set)
        }
    }

    suspend fun isChatPinSet(): Boolean = globalDataStore.isChatPinSetFlow().first()
    suspend fun setChatPin(pin: String) =
        globalDataStore.setChatPin(EncryptionManager.encrypt(pin.sha256())) // mirrors setUserAppLock
    suspend fun verifyChatPin(pin: String): Boolean =
        globalDataStore.getChatPinFlow().first()?.let { EncryptionManager.decrypt(it) == pin.sha256() } ?: false
}
```

Flow when opening a HIGHLY_SENSITIVE conversation (state machine §4):

```
open → Locked
  → tap "Unlock"  → ConversationAuthenticator.authenticate()
        ├─ biometric/device-credential available → BiometricPrompt
        │       success → SecureSessionManager.markUnlocked(id) → Unlocked/Visible
        │       fail/cancel → stay Locked
        └─ none available
              ├─ Chat PIN set     → EnterChatPinScreen → verify → markUnlocked
              └─ Chat PIN NOT set → "Create a Chat PIN" (spec: prompt to create) → setChatPin → markUnlocked
```

The **first time** a user picks HIGHLY_SENSITIVE for a conversation in settings (§13), if no auth method exists we immediately route to *Create Chat PIN* so the conversation is never left unprotectable. Chat PIN reuses the proven `SetLockScreenViewModel` validation rules.

---

## 8. ~~Call privacy~~ — deferred to v2

Out of scope for this iteration per request. The hooks are pre-wired though: incoming-call notifications flow through `CallNotificationManager` (`app/.../notification/CallNotificationManager.kt`), and the same `ConversationPrivacyRepository.get(id)` + `PanicModeManager.state` read used in §9 will redact caller/conversation to "Incoming Secure Call" in v2. No schema changes needed then.

---

## 9. Notification system changes

Entry point: `WireNotificationManager.observeMessageNotifications` → `MessageNotificationManager.handleNotification()` (`app/.../notification/`). We insert a **redaction pass** *before* notifications are styled/shown. This is the only place message content reaches the OS, so it's the correct choke point.

```kotlin
@Inject
class NotificationPrivacyMapper(
    private val privacyRepository: ConversationPrivacyRepository,
    private val panicModeManager: PanicModeManager,
) {
    /** Redacts a per-conversation notification bundle according to effective level. */
    suspend fun redact(conversation: NotificationConversation): NotificationConversation {
        val base = privacyRepository.get(conversation.id.toConvId()).level
        val panic = panicModeManager.state.value is PanicModeState.Active
        return when (PrivacyResolver.resolve(base, panic)) {
            NORMAL -> conversation

            SENSITIVE, SENSITIVE_PANIC -> conversation.copy(
                // keep name, drop every message body → "New message in <name>"
                messages = listOf(NotificationMessage.Comment(
                    author = NotificationMessageAuthor("", null),
                    time = conversation.messages.maxOf { it.time },
                    textResId = R.string.notification_sensitive_new_message, // "New message"
                ))
            )

            HIGHLY_SENSITIVE, HIGHLY_SENSITIVE_PANIC -> conversation.copy(
                name = null,                                  // hide conversation identity
                isOneToOneConversation = true,                // suppress group title rendering
                messages = listOf(NotificationMessage.Comment(
                    author = NotificationMessageAuthor("", null),
                    time = conversation.messages.maxOf { it.time },
                    textResId = R.string.notification_secure_new_message, // "New Secure Message"
                ))
            )
        }
    }
}
```

Wiring in `MessageNotificationManager.handleNotification()` — map each `NotificationConversation` through `redact()` before `getConversationNotification(...)` builds the `MessagingStyle`. Because notification IDs are `getConversationNotificationId(convId, userId)` (`NotificationConstants.kt:60`), redacted notifications still update/replace correctly per conversation.

Resulting behavior matrix:

| Effective level | Notification shows |
|---|---|
| NORMAL | sender + conversation name + preview (unchanged) |
| SENSITIVE / SENSITIVE_PANIC | conversation name + "New message" (no body, no sender line) |
| HIGHLY_SENSITIVE / *_PANIC | "New Secure Message" only — no name, no sender, no body |

Edge: the **summary** notification (per-user group) must also be neutral — when any contributing conversation is highly-sensitive, the summary text is generic ("New messages"). A global notification-privacy floor (`notif_privacy_default`, §1.2) can force *all* notifications to the highly-sensitive style regardless of per-conversation level, satisfying "Global → Notification privacy settings."

---

## 10. Auto-lock / auto-hide implementation

Covered mechanically by `SecureSessionManager` (§6). Composition in the chat screen:

```kotlin
@Composable
fun ConversationScreen(viewModel: ConversationViewModel) {
    val access by viewModel.accessState.collectAsStateWithLifecycle() // from SecureSessionManager.observeAccessState(id)

    Box {
        // Real content only composed/decrypted when actually visible.
        if (access is ConversationAccessState.Visible) {
            MessageList(
                modifier = Modifier.pointerInput(Unit) {            // any touch = activity ping
                    awaitEachGesture { awaitFirstDown(); viewModel.onUserActivity() }
                },
                state = viewModel.messages, // paged
            )
        }
        when (access) {
            ConversationAccessState.Concealed -> BlurOverlay(onTapReveal = viewModel::onReveal)   // SENSITIVE
            ConversationAccessState.Locked    -> LockedOverlay(onUnlock = viewModel::onRequestAuth) // HIGHLY
            ConversationAccessState.Authenticating -> AuthenticatingOverlay()
            ConversationAccessState.Visible   -> Unit
        }
    }
}
```

- **SENSITIVE → `BlurOverlay`**: a `Modifier.blur()` + tap-to-reveal scrim. No decryption is undone (content is still loaded), it's a visual veil — appropriate because SENSITIVE is "hide when I'm not looking," not "protect from device access."
- **HIGHLY_SENSITIVE → `LockedOverlay`**: when `Locked`, **`MessageList` is not composed at all** — decrypted content is never put on screen, and the secure cleanup (§11) has already purged caches. This is the structural difference: SENSITIVE hides pixels, HIGHLY_SENSITIVE withholds the data.

`viewModel.onReveal()`/`onUserActivity()` call `secureSessionManager.userActivity(id)` to reset the inactivity timer; `onRequestAuth()` runs §7.

---

## 11. Memory / cache cleanup strategy

On entering `Locked` for a highly-sensitive conversation, `SecureSessionCleaner.purge(id)` runs:

```kotlin
@Inject
class SecureSessionCleaner(
    private val imageLoader: ImageLoader,             // Coil, app singleton
    @CurrentAccount private val userId: UserId,
) {
    suspend fun purge(id: ConversationId) {
        // 1. Drop decrypted media from Coil memory cache for this conversation's keys.
        //    Media is keyed by asset id; we tag conversation media keys with a prefix so we can target them.
        imageLoader.memoryCache?.let { mc ->
            mc.keys.filter { it.key.startsWith("conv:${id.value}:") }.forEach { mc.remove(it) }
        }
        // 2. Decrypted message text: held only in the ConversationViewModel's paged state, which is
        //    cleared by NOT composing MessageList (§10) and by resetting the pager when state == Locked.
        // 3. Decrypted-on-disk media: Kalium writes decrypted assets to a cache dir; request deletion
        //    of this conversation's decrypted asset files via the existing asset cache API.
        // 4. App-switcher preview already covered by FLAG_SECURE (§12) — no thumbnail captured.
    }
}
```

Realistic guarantees (and honest limits):
- **Coil memory + disk caches**: deterministically cleared for the conversation's tagged keys.
- **Decrypted message text in the ViewModel**: dereferenced by clearing the paging state and not composing the list; eligible for GC. We cannot force-zero JVM `String`s (immutable, GC-managed) — this is a documented platform limitation. Sensitive *plaintext* therefore lives only as long as a GC cycle after lock, which is acceptable for the threat model (the encrypted source remains in SQLCipher).
- **App-switcher previews**: `FLAG_SECURE` (§12) prevents the OS from snapshotting the window, so no plaintext reaches the recents thumbnail.
- **Clipboard**: when locking, if the last copy originated from a highly-sensitive conversation, clear the clipboard (Android 12-: `ClipboardManager.clearPrimaryClip()`).

---

## 12. App-switcher protection (FLAG_SECURE)

Extends the existing `WireActivity.HandleScreenshotCensoring()` (`WireActivity.kt:521-529`), which already toggles `FLAG_SECURE` based on `globalAppState.screenshotCensoringEnabled`. We OR-in a new privacy condition.

```kotlin
// WireActivityViewModel: derive whether the secure window flag must be on.
val secureWindowRequired: StateFlow<Boolean> = combine(
    currentScreenManager.currentScreenFlow(),     // is a conversation open? which one?
    privacyRepository.observeAll(),
    panicModeManager.state,
) { screen, levels, panic ->
    val openConvLevel = (screen as? CurrentScreen.Conversation)?.let { levels[it.id]?.level }
    val panicActive = panic is PanicModeState.Active
    // Secure when panic is on, OR an open conversation is SENSITIVE/HIGHLY_SENSITIVE.
    panicActive && (levels.values.any { it.level != NORMAL }) ||
        (openConvLevel != null && openConvLevel != ConversationPrivacyLevel.NORMAL)
}.stateIn(...)
```

```kotlin
// WireActivity (compose effect), replacing/joining HandleScreenshotCensoring:
val secure by viewModel.secureWindowRequired.collectAsStateWithLifecycle()
LaunchedEffect(secure, censoringEnabled) {
    if (secure || censoringEnabled) window.addFlags(FLAG_SECURE) else window.clearFlags(FLAG_SECURE)
}
```

Result: opening a SENSITIVE/HIGHLY_SENSITIVE conversation, or turning on Panic Mode, blanks the recents thumbnail and blocks screenshots/screen-recording of the window — directly satisfying "hide app switcher previews."

---

## 13. UI / UX & settings screens

### 13.1 Conversation list (`ConversationItem` + `ConversationMapper`)

Add **one** field to the UI model (`ConversationItem.kt:37`):

```kotlin
sealed interface ConversationItem : ConversationItemType {
    // ...existing...
    val effectivePrivacy: EffectivePrivacyLevel   // NEW (defaults NORMAL)
}
```

The list ViewModel (`ConversationListViewModelImpl`) `combine`s the existing conversation flow with `privacyRepository.observeAll()` + `panicModeManager.state`, and `ConversationMapper.toConversationItem()` resolves `effectivePrivacy`. Rendering changes in `ConversationItemFactory.kt` / `LastMessageSubtitle.kt`:

| effectivePrivacy | Title | Subtitle |
|---|---|---|
| NORMAL | conversation name | message preview (unchanged) |
| SENSITIVE / SENSITIVE_PANIC | conversation name | `••••••••••••••••` |
| HIGHLY_SENSITIVE / *_PANIC | "Hidden Conversation" + lock glyph | `••••••••••••••••` |

```kotlin
// In ConversationItemFactory subtitle composition:
when (conversation.effectivePrivacy) {
    NORMAL -> LastMessageSubtitle(conversation.lastMessageContent, ...)
    SENSITIVE, SENSITIVE_PANIC,
    HIGHLY_SENSITIVE, HIGHLY_SENSITIVE_PANIC -> RedactedSubtitle() // renders "••••••••••••••••"
}
// title:
val title = if (conversation.effectivePrivacy.hidesIdentityInList)
    stringResource(R.string.privacy_hidden_conversation) else conversation.displayName
```

A small lock/shield affordance on the row signals the protected tier (and panic banner globally).

### 13.2 Per-conversation settings (Group / 1-to-1 details)

Add a "Privacy" section to the conversation details screen (sibling to receipt-mode / self-deletion, `GroupConversationDetailsScreen.kt`):

```
Privacy
├─ Privacy Level                    [ Normal ▸ ]   → radio sheet: Normal / Sensitive / Highly Sensitive
└─ Auto-Lock (shown if not Normal)  [ Immediately ▸ ]  → Immediately / 30s / 1m / 5m
```

Picking **Highly Sensitive** with no auth method set → immediately launches *Create Chat PIN* (§7) before persisting.

### 13.3 Global privacy settings (extend `PrivacySettingsScreen`)

The existing `PrivacySettingsScreen` (read receipts, typing, screenshot censoring, analytics) gains:

```
Panic Mode
├─ 🛡 Panic Mode                       [ toggle ]   (mirrors PanicModeManager.state)
├─ Default duration                    [ 15 min ▸ ]  → 15 min / 1 hour / Until disabled
Authentication
├─ Unlock method                       Face ID / Fingerprint / Device Passcode / Chat PIN
├─ Set / change Chat PIN               ▸
Notifications
├─ Hide content for private chats      [ toggle ]   (global notif floor, §9)
```

### 13.4 Panic Mode entry points & indicator

- **Quick action**: a shield FAB/menu item on the conversation list; one tap activates with the default duration, long-press opens the duration picker.
- **Indicator**: persistent top banner `🛡 Panic Mode Active · 14:32 left` (countdown from `state.expiresAtEpochMs`), tap to disable.
- **Profile menu / Settings**: toggle + duration as in §13.3.

UX intent mapping (spec):
- *Sensitive* = "hide content when I'm not actively looking" → blur-on-leave, dotted previews, no friction.
- *Highly Sensitive* = "only I can access this" → auth wall, purge on lock.
- *Panic* = "others may see my screen — protect everything now" → one tap escalates both tiers globally and instantly.

---

## 14. Component structure (file map)

```
app/src/main/kotlin/com/wire/android/feature/privacy/
├── model/
│   ├── ConversationPrivacyLevel.kt        # enums + ConversationPrivacySettings (pure)
│   ├── EffectivePrivacyLevel.kt           # derived enum + flags
│   └── PrivacyResolver.kt                 # pure resolve(base, panic)  ← unit-tested
├── data/
│   ├── ConversationPrivacyStore.kt        # per-user DataStore wrapper (encrypted JSON map)
│   ├── ConversationPrivacyRepository.kt    # interface + Impl
│   └── PrivacyDataStoreModule.kt          # Metro @BindingContainer, @CurrentAccount provider
├── panic/
│   └── PanicModeManager.kt                # @SingleIn(AppScope), StateFlow + timer
├── session/
│   ├── SecureSessionManager.kt            # access-state machine, auto-lock
│   └── SecureSessionCleaner.kt            # cache/memory purge
├── auth/
│   └── ConversationAuthenticator.kt       # biometric + Chat PIN
└── notification/
    └── NotificationPrivacyMapper.kt       # redaction pass

Touched existing files:
  ui/home/conversationslist/model/ConversationItem.kt        (+ effectivePrivacy)
  mapper/ConversationMapper.kt                               (resolve + map)
  ui/home/conversationslist/common/ConversationItemFactory.kt (redacted rendering)
  notification/MessageNotificationManager.kt                 (redact() pass)
  notification/WireNotificationManager.kt                    (inject mapper)
  ui/WireActivity.kt                                         (secureWindowRequired → FLAG_SECURE)
  ui/home/settings/privacy/PrivacySettingsScreen.kt          (+ panic / auth / notif sections)
  ui/home/conversations/details/GroupConversationDetailsScreen.kt (+ Privacy section)
  di/metro/WireApplicationGraph.kt                           (register PrivacyModule)
```

---

## 15. API / data-layer changes

- **Kalium (submodule): NONE.** No new use cases, no schema, no network. (This is a hard requirement from §0.1.)
- **App data layer:**
  - New `ConversationPrivacyStore` (DataStore) + `ConversationPrivacyRepository`.
  - `GlobalDataStore`: + `isPanicActive()/panicExpiresAt()/setPanic()`, `isChatPinSetFlow()/getChatPinFlow()/setChatPin()`, `notifPrivacyDefault()`.
  - Metro: a `PrivacyModule` `@BindingContainer` registered in `WireApplicationGraph` (`di/metro/WireApplicationGraph.kt`). `ConversationPrivacyStore` is provided in the **session/current-account** scope keyed by `@CurrentAccount UserId` (like `UserDataStoreProvider`); `PanicModeManager`, `SecureSessionManager`, `SecureSessionCleaner` are `@SingleIn(AppScope::class)`.

---

## 16. iOS implementation details (parallel design)

The cross-platform contract is identical: local store + effective-level resolver + panic singleton. iOS specifics:

| Concern | Android | iOS |
|---|---|---|
| Local store | per-user DataStore (encrypted JSON map) | per-user file in **Keychain** (kSecAttrAccessibleWhenUnlockedThisDeviceOnly) or encrypted plist; never iCloud-synced |
| Panic state | `PanicModeManager` `StateFlow` | `PanicModeStore` `ObservableObject` / Combine `@Published`; expiry persisted in Keychain |
| App background / focus | `CurrentScreenManager.isAppVisibleFlow()` | `ScenePhase` (`.background`/`.inactive`) via `@Environment(\.scenePhase)`; `UIApplication` notifications |
| Device lock | `ScreenStateObserver` | `UIApplication.protectedDataWillBecomeUnavailableNotification` / `isProtectedDataAvailable` |
| App-switcher hide | `FLAG_SECURE` | **No FLAG_SECURE on iOS** — add a privacy overlay window on `sceneWillResignActive` (blur `UIVisualEffectView` covering the key window), removed on `sceneDidBecomeActive` |
| Auth | `BiometricPrompt` (BIOMETRIC_STRONG \| DEVICE_CREDENTIAL) | `LAContext.evaluatePolicy(.deviceOwnerAuthentication)` (Face ID / Touch ID / passcode); Chat PIN stored in Keychain |
| Notifications | redact in `MessageNotificationManager` | redact in **Notification Service Extension** (`UNNotificationServiceExtension`) — rewrite title/body before delivery; for highly-sensitive, set `hiddenPreviewsBodyPlaceholder` / suppress via `UNNotificationContent` |
| Secure cleanup | Coil cache + deref | `URLCache`/`SDWebImage`/Nuke cache removal for the conversation; nil out view-model state |

iOS notification redaction nuance: the NSE has a hard CPU/time budget and the privacy map must be readable from the extension — store it in a **shared App Group container** (Keychain access group) so both the app and the NSE can read the effective level. Highly-sensitive ⇒ replace with "New Secure Message"; sensitive ⇒ keep name, drop body.

---

## 17. Edge cases

1. **Conversation deleted while non-NORMAL** → orphan map entry. `ConversationPrivacyStore` prunes ids no longer present on a periodic/login reconcile against Kalium's conversation list.
2. **Panic expires while a highly-sensitive chat is open** → effective level drops `*_PANIC → HIGHLY_SENSITIVE`; it does **not** auto-unlock (user must still re-auth if it locked). De-escalation never *reduces* protection of an already-locked session.
3. **Panic activates while a normal chat is open** → unaffected (spec: normal unchanged). Window flag still goes secure only if some non-normal conversation exists (we keep it scoped to avoid surprising normal-only users — configurable).
4. **No biometrics, no device passcode, user picks Highly Sensitive** → forced Chat PIN creation; if they cancel, the level change is **not** persisted (never leave it unprotectable).
5. **App killed during panic** → recomputed on cold start from `panic_mode_expires_at`; expired ⇒ auto-off, still-active ⇒ re-armed timer.
6. **Multi-account** → privacy store is per-user; panic mode + chat PIN are **global** (device-level), matching "global privacy switch."
7. **Notification arrives for highly-sensitive while unlocked & on screen** → still redacted in the tray (the tray is outside the protected window); but in-app it shows normally.
8. **Quick reply from a redacted notification** → disabled for highly-sensitive (no inline reply action), since replying would require composing in an unauthenticated context. Sensitive keeps reply.
9. **Inactivity timer + active media playback / typing** → `userActivity()` pings on scroll/touch/keystroke and during audio playback keep-alive, so the screen doesn't lock mid-interaction.
10. **`IMMEDIATELY` auto-lock + transient focus loss** (permission dialog, system sheet) → debounce ~300 ms before locking to avoid locking on benign overlays; panic mode bypasses the debounce (immediate).
11. **Search / global indexes** → highly-sensitive conversations are excluded from in-app message search results previews while locked.
12. **Downgrade level** Highly→Normal → run `SecureSessionCleaner.purge` once and drop the unlocked flag, so cached plaintext from the protected era is cleared.

---

## 18. Security considerations

- **Threat model**: shoulder-surfing, screen-sharing/recording, recents thumbnails, brief unattended device, shared device. **Not** in scope: a forensic attacker with root + the SQLCipher key (that's the platform's existing E2EE/at-rest story).
- **Metadata at rest**: the privacy map is encrypted (`EncryptionManager`) so file access doesn't reveal which conversations are sensitive.
- **No backend leakage**: levels never leave the device — no API field, no analytics event carrying conversation identity + level. Panic activation must **not** be logged with conversation specifics on non-debug flavors.
- **FLAG_SECURE coverage**: applies to the whole window when a non-normal conversation is open or panic is on; verify it also covers any `Dialog`/`PopupWindow` (separate windows need the flag set independently).
- **Auth strength**: prefer `BIOMETRIC_STRONG`; Chat PIN is hashed (`sha256`) + encrypted, never stored plaintext, and rate-limited (reuse `EnterLockScreenViewModel` lockout behavior — `ERROR_LOCKOUT`).
- **Plaintext lifetime**: documented limit — JVM strings can't be zeroed; mitigated by not composing content when locked and purging caches. Consider `CharArray`-based decryption for the most sensitive media paths if the threat model tightens.
- **Bypass via notifications**: redaction is server-agnostic and applied at the single OS choke point; the summary notification is neutralized too (§9).
- **Panic cannot be weakened by force-quitting** (persisted expiry).

---

## 19. Testing strategy (Arrangement-builder pattern, per AGENTS.md)

- **`PrivacyResolverTest`** — exhaustive base×panic matrix → effective level. Pure, no Android.
- **`PanicModeManagerTest`** — activate/expire/persist/cold-start recompute, using a fake `CurrentTimeProvider` (the abstraction `LockCodeTimeManager` already uses) and `runTest` virtual time. `givenPanicActivatedFor15Min_whenTimerExpires_thenStateBecomesInactive`.
- **`SecureSessionManagerTest`** — drive `isAppVisibleFlow`/`screenOnFlow`/effective-level fakes; assert state transitions and that `purge` fires on lock. Turbine on `observeAccessState`.
- **`ConversationPrivacyRepositoryTest`** — map persistence, default=NORMAL, `observeEffective` combines with panic.
- **`NotificationPrivacyMapperTest`** — each level redacts correctly; summary neutralized.
- **ViewModel tests** — list ViewModel shows dotted preview / hidden title; details ViewModel routes to Chat-PIN creation when none set. All via `Arrangement().withLevel(...).withPanic(...).arrange()`.
- **Screenshot tests** (`:app:validateStagingDebugScreenshotTest`) — `RedactedSubtitle`, `BlurOverlay`, `LockedOverlay`, panic banner.
- Run `./gradlew :app:testDevDebugUnitTest` and `./gradlew staticCodeAnalysis` before finishing.

---

## 20. Rollout & ADR

- Behind a `BuildConfig` feature flag (per-flavor), default on for `dev`/`staging` first.
- **ADR required**: introducing the per-conversation local-privacy store + the secure-session/panic architecture is a new pattern → add `docs/adr/0010-conversation-privacy-and-panic-mode.md` before implementation (per AGENTS.md §5). No new third-party dependency is introduced (DataStore, BiometricPrompt, Coil, Metro are all already present), which keeps the ADR scoped to *pattern*, not *library*.

---

### Implementation order (suggested)

1. Models + `PrivacyResolver` (+ tests).
2. `ConversationPrivacyStore` + repository (+ tests).
3. `PanicModeManager` + GlobalDataStore keys (+ tests).
4. List integration (`ConversationItem`/`Mapper`/`ItemFactory`) — visible value early.
5. `SecureSessionManager` + chat-screen overlays + `SecureSessionCleaner`.
6. `ConversationAuthenticator` + Chat PIN screens.
7. `NotificationPrivacyMapper` wiring.
8. `FLAG_SECURE` extension in `WireActivity`.
9. Settings screens (per-conversation + global) + panic entry points/indicator.
10. Edge-case hardening + screenshot tests.