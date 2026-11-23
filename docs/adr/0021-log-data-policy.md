# 21. Log data policy for the Android app

Date: 2026-11-23

## Status

Proposed

Depends on Kalium [ADR 0018](https://github.com/wireapp/kalium/blob/develop/docs/adr/0018-log-data-policy.md), which is also Proposed. This ADR cannot be accepted before it.

## Context

Kalium owns the shared log data policy, the table of what may appear in a log. Android does not copy that table. This ADR records which Android code owns each place a log can end up, so the policy has a named owner at every sink.

The app builds its writer list in `WireApplication` (`fullLoggerConfig` and `minimalLoggerConfig`). Each writer receives the message, tag and throwable unchanged. There is no shared choke point.

[ADR 15](0015-migrate-legacy-diagnostic-logs.md) decided that diagnostic logs are written directly to a rolling file and exported as one ZIP, including a legacy gzip snapshot. That decision is not revisited here.

## Decision

Android follows Kalium ADR 0018 at every sink listed below. Android owns all of them.

| Sink | Code | Owner |
| --- | --- | --- |
| Logcat | `platformLogWriter()` in `WireApplication`. It stays active at WARN when diagnostic logging is off. | Android app |
| Rolling files | `LogFileWriterImpl` | Android app |
| Shared diagnostics ZIP | `LogSharing` copies the rolling files and the legacy snapshot into the ZIP. | Android app |
| DataDog | `DataDogLogger` in the `private` source set. The `public` source set has a no-op writer. | Android app |

Kalium owns the policy table and, once built, the shared enforcement code. Android owns the writer list, the four sinks above and the migration of its own call sites.

ADR 0015 still applies until LOG-1.3 decides how retained logs and exports are handled. Until then, `LogSharing` exports the files as they were written, and the legacy snapshot stays.

### Logs before the logger exists

`WireApplication` writes to `android.util.Log` directly for startup tracing, the crash flush, app exit monitoring and the previous exit reason. These calls run before the Kalium logger is set up or outside it. Today they hold fixed text, an exit reason and timestamps. Two calls also pass an exception (`Log.e` with the caught exception in the crash flush and exit monitoring setup), so exception text can reach logcat there. These logs must stay limited to fixed text, codes and timestamps. Any change that adds a dynamic value needs review against ADR 0018.

### Open decision

How IDs are logged is open in ADR 0018: in clear, omitted or as per-install pseudonyms. Android takes whatever that ADR accepts. The recommendation there is to omit IDs by default and use per-install pseudonyms where a documented diagnostic need exists. That recommendation awaits team acceptance.

## Consequences

- Each sink has one named owner, so a leak at a sink has a known place to fix.
- ADR 0015 stays valid, so exports keep the legacy snapshot and old files stay unsanitized until LOG-1.3.
- The direct `android.util.Log` calls stay outside the sanitizing path. A static check can keep them to fixed content, but until then review is the only guard.
- DataDog is a private-flavor sink. Policy checks must cover that source set, not only the public one.
