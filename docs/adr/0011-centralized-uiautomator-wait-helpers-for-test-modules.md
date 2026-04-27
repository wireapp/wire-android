# 11. Centralized UIAutomator Wait Helpers for Test Modules

Date: 2026-04-23

## Status

Accepted

## Context

The `:tests:testsCore` and `:tests:testsSupport` modules had duplicated synchronization logic:
- manual polling loops
- ad-hoc sleeps
- repeated "wait until visible/gone" patterns
- repeated stale-object retry logic

This duplication made tests harder to maintain and increased the risk of inconsistent behavior and flaky waits across page objects and critical flows.

## Decision

We centralize reusable wait/select/retry behavior in `:tests:testsSupport` under `uiautomatorutils.UiWaitUtils`, and we migrate `:tests:testsCore` callers to use those helpers.

The shared helper surface includes:
- `retryUntilTimeout(...)`
- `waitUntilVisibleOrThrow(...)`
- `waitUntilGoneOrThrow(...)` for `BySelector` and `UiSelector`
- `waitAnyVisible(...)`
- `clickWhenClickable(...)`

Compatibility wrappers remain available (`waitUntilVisible`, `waitUntilElementGone`, `waitElement`) and are internally aligned with the same reusable primitives.

## Consequences

Positive:
- less duplicated synchronization logic in `:tests:testsCore`
- more consistent wait behavior and error handling across tests
- simpler future refactors (for example Kotlin Duration migration in follow-up work)

Trade-offs:
- `UiWaitUtils` becomes the main synchronization entrypoint and must be kept well documented
- helper behavior changes can affect many tests at once, so updates require targeted regression checks
