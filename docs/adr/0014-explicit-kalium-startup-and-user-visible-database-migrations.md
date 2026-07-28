# 14. Explicit Kalium startup and user-visible database migrations

Date: 2026-07-27

## Status

Accepted

## Context

Kalium currently opens databases as a side effect of constructing scopes:

- Constructing `CoreLogic` creates the global database builder.
- Calling `getSessionScope(userId)` synchronously constructs a `UserSessionScope`.
- Constructing `UserSessionScope` creates `UserStorage` and its database driver.
- The first statement executed by `UserDatabaseBuilder` opens the database and lets the
  SQLDelight/SQLite callback run any required schema migration.

This works for small migrations, but it gives consumers no explicit way to distinguish between a
session that is opening, migrating, ready, or failed. A caller can unknowingly trigger disk work from
dependency injection, an application observer, a service, a worker, or UI construction.

There are several startup consumers that can race to open a session. In particular,
`GlobalObserversManager` starts from `WireApplication` and calls `getSessionScope()` for valid
accounts, while `WireActivity` independently resolves the current account and creates its
session-scoped Metro graph. An expensive migration can therefore begin before the activity can
present useful status, and the thread that first asks for the scope is blocked until the database is
ready.

We need to support schema migrations and post-schema data migrations that can take long enough to
block use of an account. The solution must:

- keep all database opening and migration work off the main thread;
- expose readiness, migration progress, and failure without exposing persistence implementation
  details to Android UI;
- guarantee that no session-scoped component accesses a partially migrated database;
- coalesce concurrent requests for the same user database;
- avoid showing a migration screen for normal migrations that complete in a few milliseconds;
- remain safe across activity recreation, account switching, cancellation, and process death;
- support a system notification when work is long enough to continue while the app is backgrounded;
- work in non-Firebase flavors such as F-Droid.

Firebase Cloud Messaging (FCM) is not a suitable mechanism for migration progress. FCM delivers
server-originated messages, while a database migration is local work whose start, progress, and
completion are only known by the device. The existing `WireFirebaseMessagingService` also creates
`CoreLogic` and schedules notification-fetch work, so FCM processing is another startup consumer
that must wait for the relevant database to become ready; it cannot be the source of migration
state.

Android foreground execution is a separate concept from FCM. A foreground service or a long-running
WorkManager worker displays a local ongoing notification and gives the process stronger execution
priority while important work continues. The project already uses this pattern for initial sync, and
`NotificationIds` already reserves migration-related notification IDs.

## Decision

### 1. Make Kalium database startup explicit

Kalium will expose a startup coordinator with a stable handle for each user session. The global
database is deliberately out of scope: it is small, has no long-running migrations, and stays on
the existing synchronous startup path.

```kotlin
public interface KaliumStartup {
    public fun session(userId: UserId): StartupHandle<UserSessionScope>
}

public interface StartupHandle<T> {
    public val state: StateFlow<StartupState>

    /**
     * Opens the database and runs all blocking migrations.
     *
     * The operation is idempotent and single-flight. Concurrent callers await the same work.
     */
    public suspend fun open(): StartupResult<T>

    /**
     * Explicitly retries a failed startup when the failure is classified as retryable.
     */
    public suspend fun retry(): StartupResult<T>

    /**
     * Returns a ready scope without doing disk work, or null when startup is not complete.
     */
    public fun readyOrNull(): T?
}

public sealed interface StartupState {
    public data object NotStarted : StartupState
    public data object Opening : StartupState
    public data class Migrating(val progress: MigrationProgress) : StartupState
    public data object Ready : StartupState
    public data class Failed(val failure: StartupFailure) : StartupState
}

public data class MigrationProgress(
    val stage: Stage,
    val completedUnits: Long? = null,
    val totalUnits: Long? = null,
) {
    public enum class Stage {
        Preparing,
        UpdatingSchema,
        MigratingData,
        Finalizing,
    }
}

public sealed interface StartupResult<out T> {
    public data class Success<T>(val value: T) : StartupResult<T>
    public data class Failure(val failure: StartupFailure) : StartupResult<Nothing>
}
```

The exact public names may change during implementation, but the following semantics are required:

- `open()` performs database work on Kalium's IO dispatcher.
- Each handle is single-flight. Multiple callers cannot run the same migration concurrently.
- Cancelling an awaiting UI coroutine does not cancel an in-progress SQLite migration. The migration
  is owned by an application-level supervisor and the caller only stops awaiting its result.
- A scope is cached and exposed only after all user-blocking schema and data migration steps finish.
- `Failed` contains a stable, sanitized failure category suitable for retry decisions and telemetry,
  not raw SQL, passphrases, paths, or database contents.
- Retrying is explicit and only allowed when the migration implementation says it is safe.
- Startup state does not contain Android strings. Android maps the technical stage to localized UI.

The existing synchronous `getSessionScope(userId)` API will be deprecated in favor of the startup
handle. During the compatibility period it must become ready-only and fail fast when the session has
not been prepared. It must no longer open a database or run a migration as a hidden side effect.

### 2. Report schema and data migration state

An internal migration reporter will be passed through the persistence construction path to both
Android open-helper implementations:

- the unencrypted `SqliteCallback`;
- the SQLCipher `SupportOpenHelperFactory`.

The reporter emits `UpdatingSchema` before delegating to the SQLDelight upgrade callback and emits
the next state only after that callback returns successfully.

One large SQL statement has no trustworthy fractional progress. Such work will use indeterminate
progress rather than an invented percentage.

Expensive post-schema transformations should be implemented as resumable data-migration steps:

- split work into bounded chunks where practical;
- persist a durable migration step/checkpoint;
- make each step idempotent or transactional;
- emit real completed and total units only when they are known;
- finish all user-blocking steps before publishing `Ready`.

Schema upgrades continue to rely on SQLite transaction guarantees. Post-schema steps must be able to
resume safely after process death.

### 3. Gate session-scoped Android components on readiness

Android will add an app-scoped startup coordinator/view model that maps Kalium startup state into
Android presentation state.

`WireActivity` will install Compose content before resolving the complete initial app state. The
first content uses only the application graph and can render a startup gate without constructing a
session-scoped Metro graph.

Startup order will be:

1. Render the app-scoped startup surface.
2. Read the current account from the existing global scope.
3. If there is no current account, render the authentication graph.
4. If there is a current account, open that user's session handle.
5. Resolve session-dependent startup decisions, such as E2EI enrollment.
6. Create the session-scoped Metro graph and render the normal destination only after `Ready`.
7. Start user-session observers only after the corresponding handle is ready.

The Android splash screen will remain only until the first Compose frame is available. A potentially
long migration will use a real Compose screen rather than holding the static system splash screen.

Application observers, services, workers, notification fetches, and FCM-triggered work must use the
same startup handles. They may await an already-running migration, but they must not independently
open a database. FCM-triggered notification-fetch work for a migrating user remains queued until the
session becomes ready.

#### FCM cold-start behavior

An FCM data push can be the first entrypoint into a stopped Wire process. In that case, startup will
follow this sequence:

1. `WireFirebaseMessagingService` creates the application graph and a cheap `CoreLogic`/startup
   coordinator. Opening the small global database is allowed.
2. `onMessageReceived()` validates only the push envelope, enqueues the existing unique
   `NotificationFetchWorker` for the supplied user, and returns within the short FCM callback window.
3. `NotificationFetchWorker` reads the small global database to resolve the pushed user's qualified
   ID.
4. After confirming that the pushed user has a valid session, the worker calls
   `startup.session(userId).open()`.
5. If the worker is the first caller, it starts the migration. If the activity or another consumer
   already started it, the worker awaits the same single-flight operation.
6. Only after `Ready` does the worker connect/synchronize, read pending events, and build message or
   call notifications.

The WorkManager request is the durable record that notification work is pending; the in-memory
startup handle is not the durable record. If the process is stopped during migration, WorkManager
can run the request again and the idempotent/checkpointed migration resumes before notifications are
fetched.

The existing per-user unique-work policy coalesces multiple pushes received while the migration is
running. This is safe because the eventual synchronization processes all pending events; a separate
worker is not required for every push.

The worker maps startup outcomes explicitly:

- `Ready`: continue with notification synchronization;
- retryable migration/open failure: return `Result.retry()` with WorkManager backoff;
- permanent failure: return `Result.failure()` and use the migration failure-notification policy;
- missing or logged-out session: return `Result.success()` because there is no notification work for
  that account.

The worker must not report `Result.success()` merely because an exception interrupted migration.

`onNewToken()` is unaffected because it only uses the small global storage path.

For multiple accounts, only the current account is proactively prepared by the activity. Other
accounts are prepared on demand by their entrypoint. Per-user database opening is serialized so
large migrations do not run concurrently across accounts. If an FCM worker already started another
account's migration, the current account waits for that database operation rather than competing for
I/O.

### 4. Delay migration UI to avoid flashes

Kalium emits migration state immediately so that coordination, logs, tests, and background consumers
remain accurate. Delayed visibility is an Android presentation concern and does not delay the
migration itself.

Android will use a centralized presentation policy with monotonic time:

```kotlin
internal data class BlockingWorkPresentationPolicy(
    val revealDelay: Duration = 750.milliseconds,
    val minimumVisibleDuration: Duration = 500.milliseconds,
)
```

The values are defaults and can be tuned from startup measurements without changing the Kalium API.
They are not remote-configured because startup correctness and visual stability must not depend on
network availability. Background entrypoints use a separate five-second foreground-notification
delay because that timing is an Android execution concern rather than UI presentation state.

The state machine is:

1. Start opening/migrating immediately.
2. While work is active but `revealDelay` has not elapsed, keep the lightweight startup surface
   visible without migration copy or progress.
3. If work finishes before `revealDelay`, proceed directly to the destination and never render the
   migration screen.
4. If work is still active at `revealDelay`, render the blocking migration screen.
5. Once rendered, keep it visible for at least `minimumVisibleDuration`, even if completion arrives
   immediately afterward, to prevent a one-frame flash.
6. After both `Ready` and the minimum visible duration are satisfied, transition once to the normal
   destination.
7. A failure bypasses the reveal delay when user action is required. If the failure occurs before
   the first frame, the failure surface becomes the first rendered app content.

The delayed UI collector must use `collectLatest`/cancellable timers or an equivalent state machine
so stale reveal jobs cannot display migration UI after `Ready`. The presentation state must survive
configuration changes in a view model; it must not restart the reveal delay on recomposition.

Progress is determinate only when both `completedUnits` and `totalUnits` are valid. Otherwise the UI
uses an indeterminate indicator with generic localized copy such as "Updating Wire" and a message
that the app should remain open. Schema version numbers, table names, account identifiers, and raw
errors are never displayed.

### 5. Use a local foreground notification, not FCM

Short migrations do not post a notification.

If migration remains active past `foregroundNotificationDelay`, or if the user backgrounds the app
while a visible blocking migration is active, Android promotes the work to an appropriate foreground
execution mechanism and posts a local ongoing notification. Promotion and notification creation are
owned by Android; Kalium only exposes state and progress.

When an FCM-triggered `NotificationFetchWorker` is the first caller and starts a long migration, that
worker owns the foreground execution. Its foreground notification changes from message-fetch
wording to database-maintenance wording while startup is blocked and remains until the worker
finishes. It must not post two competing foreground notifications for one worker.

On Android 8 through 11, the existing expedited WorkManager request may require an immediate
foreground notification before the migration reveal threshold. On those versions, the generic
message-fetch notification may appear immediately and can be updated to migration wording after the
threshold; the guarantee is that a short migration does not create an additional migration-specific
notification. Android 12 and newer can apply the delayed foreground promotion policy.

The foreground execution adapter may use a long-running `CoroutineWorker`, following the existing
`InitialSyncWorker` pattern, provided the worker and the activity share the same single-flight
Kalium startup handle. If Android platform restrictions make a direct foreground service more
appropriate for immediate user-initiated startup work, that choice can be made in implementation
without changing the Kalium contract.

The migration notification will:

- use a dedicated `DATABASE_MAINTENANCE_CHANNEL_ID`;
- use `IMPORTANCE_LOW`, no sound, no vibration, and no badge;
- update the worker's stable message-sync foreground notification ID rather than posting a second
  migration notification;
- be ongoing while the database is not safe to use;
- show the same determinate or indeterminate progress as the in-app surface;
- open `WireActivity` when tapped;
- not offer cancellation while a non-cancellable SQLite migration is active;
- be removed immediately when startup becomes `Ready`;
- become a non-ongoing retry notification on failure only when the app is not visible and user action
  is required.

The existing `OTHER_CHANNEL_ID` is not reused because it is created as a high-importance regular
channel. On Android 8 and newer, channel importance controls interruption behavior regardless of a
notification builder's lower priority.

The notification is local and therefore works for `dev`, `staging`, `internal`, `beta`, `prod`, and
`fdroid`. It does not require an FCM token, network access, or backend changes.

On Android 13 and newer, foreground work can start without the `POST_NOTIFICATIONS` runtime
permission, but when the permission is denied the foreground-service notice may appear only in the
system task manager rather than the notification drawer. The in-app migration screen therefore
remains the primary user feedback while Wire is visible.

Foreground execution is not started merely to display a notification. It is used only when the work
needs stronger process-lifetime guarantees. The migration remains restartable regardless, because
Android can still stop the process.

Relevant Android guidance:

- [Support for long-running workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running)
- [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [Receive messages in Android apps](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)

### 6. Verify startup and migration behavior

Kalium tests will cover:

- no-migration opening: `NotStarted -> Opening -> Ready`;
- schema migration: `NotStarted -> Opening -> Migrating -> Ready`;
- a post-schema data migration with real progress;
- failure and safe retry;
- concurrent `open()` calls invoking the database factory and migration exactly once;
- cancellation of one waiter without cancellation of the shared migration;
- no scope exposure before `Ready`;
- checkpoint resume after simulated process interruption;
- isolation between per-user handles.

Android tests will use injected durations and test dispatchers to cover:

- completion before `revealDelay` never showing migration UI;
- migration still active at the threshold showing the blocking screen;
- `minimumVisibleDuration` preventing a flash;
- stale delayed jobs not showing UI after completion;
- failure appearing without unnecessary delay;
- activity recreation preserving presentation timing;
- no session graph creation before `Ready`;
- account switching through the same gate;
- foreground notification promotion, progress updates, completion removal, and failure behavior;
- no migration notification for short work;
- FCM cold start reading only global storage before the per-user startup gate;
- FCM-triggered work awaiting session readiness rather than opening another session;
- multiple per-user pushes during migration remaining coalesced behind one worker;
- retryable startup failure returning `Result.retry()` instead of success;
- token refresh deferring database-dependent registration through durable work.

Existing SQLDelight migration verification remains in place and new expensive migrations require a
regression test using representative large datasets.

## Consequences

### Positive

- Database opening and migration become explicit, observable operations.
- Long migrations can present honest progress, retry behavior, and a safe blocking surface.
- Normal short migrations remain visually invisible.
- Single-flight startup removes races between UI, application observers, services, workers, and FCM
  processing.
- Central startup consumers no longer create session-scoped objects against a partially migrated
  database; remaining synchronous compatibility call sites are migrated incrementally.
- Current-account startup is prioritized without forcing all account databases to migrate in
  parallel.
- The notification works without Firebase and consistently across all flavors.
- Migration implementations gain a clear path toward checkpointed, restartable data transformations.

### Trade-offs

- User storage and session-scope creation must be refactored so per-user database opening is
  suspendable.
- Existing synchronous `getSessionScope()` call sites must migrate to explicit readiness handling.
- Application observers and workers need lifecycle changes so they await startup handles.
- A minimum visible duration can intentionally add up to 500 ms after a migration has completed, but
  only after the blocking screen was already shown.
- Foreground execution and notification behavior must be kept aligned with evolving Android
  restrictions and tested on supported API levels.
- Chunked post-schema migrations require durable checkpoints and more migration-specific tests.

### Follow-up implementation sequence

1. Add Kalium per-session startup handles and single-flight state machines.
2. Add migration reporting to encrypted and unencrypted database callbacks.
3. Add the app-scoped startup coordinator and delayed presentation policy.
4. Gate session Metro graph construction and user-session observers on `Ready`.
5. Migrate services, workers, and FCM-triggered notification work to startup handles.
6. Add the low-importance database-maintenance channel and foreground execution adapter.
7. Deprecate and remove database-opening behavior from synchronous session-scope access.
