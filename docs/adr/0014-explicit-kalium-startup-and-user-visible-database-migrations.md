# 14. Android integration for explicit Kalium user database preparation

Date: 2026-07-27

## Status

Accepted

## Context

Kalium currently opens user databases as a side effect of constructing scopes:

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

[Kalium ADR 10](../../kalium/docs/adr/0010-explicit-user-database-migration-lifecycle.md) owns the
shared lifecycle and correctness rules for user database preparation. It distinguishes required
schema migrations, which must finish before a session is exposed, from deferred data migrations,
which may run only while old and new code can safely coexist.

This ADR defines how Wire for Android consumes that preparation boundary. It does not define
Kalium's exported API names, result types, transaction behavior, or migration framework.

The Android integration must:

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

### 1. Consume Kalium's preparation boundary

Android will consume the app-facing session-preparation operation and observable state defined by
Kalium ADR 10. The global database remains outside this gate because it is small and stays on its
existing opening path.

Android relies on these Kalium-owned semantics:

- preparation runs on Kalium's I/O dispatcher;
- concurrent callers for the same user await one in-process preparation operation;
- cancelling one Android waiter does not cancel preparation that another entrypoint still needs;
- no `UserSessionScope` or DAO is exposed before opening, required schema migration, and verification
  succeed;
- failures use stable, sanitized categories that distinguish retryable from permanent outcomes;
- synchronous session access becomes ready-only after consumers migrate to preparation.

The exact API names and exported types remain a Kalium implementation decision. Android maps the
technical state to app-owned presentation state and localized strings.

In this ADR, `Ready` and `Failed` name Android presentation outcomes. They do not prescribe Kalium's
exported type or case names.

Deferred data migrations do not participate in this blocking startup gate, migration screen, or
foreground-notification policy. Android may schedule them separately after Kalium defines a concrete
shared deferred-migration API.

Required schema migration progress is indeterminate unless Kalium can report trustworthy completed
and total units. Android must never derive a percentage from elapsed time, schema versions, or an
assumed row count.

### 2. Gate session-scoped Android components on readiness

Android will add an app-scoped startup coordinator/view model that maps Kalium preparation state into
Android presentation state.

`WireActivity` will install Compose content before resolving the complete initial app state. The
first content uses only the application graph and can render a startup gate without constructing a
session-scoped Metro graph.

Startup order will be:

1. Render the app-scoped startup surface.
2. Read the current account from the existing global scope.
3. If there is no current account, render the authentication graph.
4. If there is a current account, invoke Kalium's session-preparation operation for that user.
5. Resolve session-dependent startup decisions, such as E2EI enrollment.
6. Create the session-scoped Metro graph and render the normal destination only after `Ready`.
7. Start user-session observers only after the corresponding preparation is ready.

The Android splash screen will remain only until the first Compose frame is available. A potentially
long migration will use a real Compose screen rather than holding the static system splash screen.

Application observers, services, workers, notification fetches, and FCM-triggered work must use the
same Kalium preparation boundary. They may await an already-running operation, but they must not
independently open a database. FCM-triggered notification-fetch work for a user being prepared
remains queued until the session becomes ready.

#### FCM cold-start behavior

An FCM data push can be the first entrypoint into a stopped Wire process. In that case, startup will
follow this sequence:

1. `WireFirebaseMessagingService` creates the application graph and obtains Kalium's cheap
   session-preparation entrypoint. Opening the small global database is allowed.
2. `onMessageReceived()` validates only the push envelope, enqueues the existing unique
   `NotificationFetchWorker` for the supplied user, and returns within the short FCM callback window.
3. `NotificationFetchWorker` reads the small global database to resolve the pushed user's qualified
   ID.
4. After confirming that the pushed user has a valid session, the worker invokes Kalium's
   session-preparation operation.
5. If the worker is the first caller, it starts the migration. If the activity or another consumer
   already started it, the worker awaits the same single-flight operation.
6. Only after `Ready` does the worker connect/synchronize, read pending events, and build message or
   call notifications.

The WorkManager request is the durable record that notification work is pending; in-memory
preparation state is not that durable record. If the process is stopped during a required migration,
WorkManager can run the request again. Kalium re-inspects the durable schema version and safely
retries from the last committed version before notifications are fetched. WorkManager state is never
proof that database preparation completed.

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
accounts are prepared on demand by their entrypoint. Android gives visible current-account startup
scheduling priority: queued background work for other accounts must not start new preparation while
the current account is waiting. An operation that already started is not cancelled or preempted.
Kalium guarantees single-flight preparation per user; this ADR does not require process-wide
serialization across different user databases. Any cross-account I/O concurrency limit must be
based on measurements and preserve visible current-account priority.

### 3. Delay migration UI to avoid flashes

Kalium emits preparation state immediately so that coordination, logs, tests, and background
consumers remain accurate. Delayed visibility is an Android presentation concern and does not delay
preparation itself.

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

### 4. Use a local foreground notification, not FCM

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
Kalium preparation operation. If Android platform restrictions make a direct foreground service more
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

### 5. Verify startup and migration behavior

Kalium ADR 10 requires tests that cover:

- opening without a migration;
- required schema migration and verification before readiness;
- failure and safe retry;
- concurrent preparation calls invoking the database factory and migration exactly once;
- cancellation of one waiter without cancellation of the shared migration;
- no scope exposure before `Ready`;
- restart from the last committed schema version after simulated process interruption;
- isolation between per-user preparation operations.

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
- Visible current-account startup is prioritized when Android schedules new preparation work.
- The notification works without Firebase and consistently across all flavors.
- Interrupted required migrations recover from Kalium's durable schema-version source of truth.

### Trade-offs

- User storage and session-scope creation must be refactored so per-user database opening is
  suspendable.
- Existing synchronous `getSessionScope()` call sites must migrate to explicit readiness handling.
- Application observers and workers need lifecycle changes so they await session preparation.
- A minimum visible duration can intentionally add up to 500 ms after a migration has completed, but
  only after the blocking screen was already shown.
- Foreground execution and notification behavior must be kept aligned with evolving Android
  restrictions and tested on supported API levels.
- Deferred data migrations require a separate Kalium contract and Android scheduling policy when a
  concrete use case is introduced.

### Follow-up implementation sequence

1. Implement and adopt Kalium's session-preparation boundary according to Kalium ADR 10.
2. Add the app-scoped startup coordinator and delayed presentation policy.
3. Gate session Metro graph construction and user-session observers on readiness.
4. Migrate services, workers, and FCM-triggered notification work to the preparation boundary.
5. Add the low-importance database-maintenance channel and foreground execution adapter.
6. Remove database-opening behavior from synchronous session-scope access after consumer migration.

## References

- [Kalium ADR 10: Explicit lifecycle for long-running user database migrations](../../kalium/docs/adr/0010-explicit-user-database-migration-lifecycle.md)
