# 21. Log data policy

Date: 2026-11-25

## Status

Proposed

## Context

What a log may contain was never written down. Kalium writes most of the logs and owns the shared logger, so it records the policy in [ADR 0018](https://github.com/wireapp/kalium/blob/develop/docs/adr/0018-log-data-policy.md). It sets the rules for credentials, IDs, domains, hosts, query values, exception messages and content.

The app adds its own log writers and decides where logs go. It needs to say which sinks exist, who owns each one, and how they follow the policy.

## Decision

The app follows Kalium's [log data policy](https://github.com/wireapp/kalium/blob/develop/docs/adr/0018-log-data-policy.md). A change to the table goes through the Kalium ADR.

App code logs through `AppLogger`, which feeds the same writers as Kalium's `CoreLogger`. Logging is on by default on `dev`, `alpha` and `beta` builds (`logging_enabled` in `default.json`) and off on `prod` and `fdroid` until the user turns it on. These are the sinks:

| Sink | What it is | When it runs | Owner |
| --- | --- | --- | --- |
| Logcat | `platformLogWriter()` | Always. Warn and above, or verbose when logging is on. | App (`WireApplication`) |
| Rolling log files | `LogFileWriterImpl` writes `wire_logs.log` and its rolls to `cacheDir/logs` | When logging is on | App (`util/logging`) |
| Support ZIP | `CompressedLogsArchiveCreator` zips the logs directory, and `LogShareLauncher` shares it | When the user shares logs or reports a bug | App (`util/logging/LogSharing.kt`) |
| DataDog logs | `DataDogLogger` | Only on `dev`, `alpha` and `beta` builds, and only if logging is on when the app starts. The `public` source set used by `prod` and `fdroid` is a no-op. | App (`src/private`, `src/public`) |
| DataDog crash reports and RUM | DataDog SDK, set up in `ExternalLoggerManager` | On every `dev`, `alpha` and `beta` build at startup (`WireApplication.initializeApplicationLoggingFrameworks`), whether logging is on or off. It does not go through the log writers. | App (`src/private`) |

Every log writer receives the same message, so the producer must apply the policy before it logs. A sink does not sanitize on its own.

[ADR 0015](0015-migrate-legacy-diagnostic-logs.md) still applies to retained log files: direct file logging, the legacy snapshot and the single ZIP export.

## Consequences

Reviews and lint rules for app code point at one table in Kalium. The sink list shows where a leak ends up and who to ask.

`alpha` and `beta` builds use the production backend, so their DataDog logs hold the same kind of data as prod logs. They follow the policy now because Kalium does not know the build type.

The log tag breaks the policy today. Kalium builds it from the full user ID, domain and client ID, and every log writer receives it. The rolling files and the support ZIP write it out, and `DataDogLogger` also sends `userId` and `clientId` as attributes. This needs a follow-up change in Kalium and in `DataDogLogger`.

DataDog is attached once, at startup. Turning logging on later does not add it, and turning logging off leaves it attached until the app restarts. This needs a follow-up change so the toggle takes effect at once.

RUM tracks activities with `trackExtras = true`, so it sends `Intent` extras to DataDog on internal builds even with logging off. An extra that carries an ID or other user data reaches DataDog without passing through the policy.

Logs already on devices and in support bundles, written before the policy, are not rewritten. Whether to change how retained logs and exports work is a later decision. ADR 0015 stays in force until then.
