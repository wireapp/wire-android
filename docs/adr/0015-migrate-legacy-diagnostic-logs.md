# 15. Replace logcat-process capture with direct diagnostic file logging

Date: 2026-09-02

## Status

Accepted

## Context

The previous Android diagnostic logger started a `logcat` process and copied its output into
`wire_logs.txt`, with timestamped gzip rolls. That approach depends on device-specific logcat
availability, permissions and process behavior. It can produce empty diagnostic files on some
OEM/vendor devices even while the application is running normally, which makes support reports
unreliable precisely where logs are needed most.

Application and Kalium diagnostics already flow through Kermit. Writing that stream directly to a
rolling file avoids relying on the platform logcat process. The direct writer stores
`wire_logs.log` and numbered rolls in the same directory, so upgraded installations also need a
safe transition for existing legacy files.

The current app packages diagnostic files into a single ZIP archive. This established export
behavior must be retained while preserving a useful legacy diagnostic snapshot and avoiding data
loss if migration is interrupted or filesystem operations fail.

## Decision

The app writes Kermit diagnostics directly to a rolling file and does not use a `logcat` process
for diagnostic file collection. Application and Kalium log writers feed the same direct writer,
subject to the existing logging enablement and log-level configuration.

When direct file logging starts, the app migrates a legacy `wire_logs.txt` file before opening the
rolling writer.

- Compress the legacy active file into the deterministic `wire_legacy_active.gz` through a
  temporary sibling file.
- Validate and finalize the gzip snapshot before deleting the legacy source.
- After a valid snapshot exists, remove only recognized legacy archives and temporary remnants.
- On startup after an interruption, treat a valid snapshot as the committed state and finish
  cleanup without creating another snapshot.
- If compression or finalization fails, retain the legacy source and history, then continue with
  direct logging.
- Delete logs removes recognized legacy files, the retained snapshot, and direct rolling files;
  unrelated files in the directory are preserved.
- Diagnostic sharing remains a single ZIP archive. The snapshot is included naturally as an entry
  alongside current rolling logs.

## Consequences

Diagnostic logging no longer depends on an OEM's logcat process behavior, so files contain the
events emitted by the application's configured Kermit writers on supported devices. Users retain
at most one compressed view of the legacy active log after upgrade, and support exports contain
both pre-upgrade and current diagnostics without changing their ZIP format.

The direct approach records only logs routed through Kermit; it intentionally does not capture
arbitrary system or third-party logcat output. Migration adds a small amount of filesystem work on
the first enabled direct-logging startup and requires focused tests for successful migration,
failed finalization, restart recovery, deletion, and ZIP contents. The filename allow-list must
be maintained if diagnostic log naming changes.
