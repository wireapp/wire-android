## Focus areas (light review)

Read the diff and the changed files. Read other files only when a finding needs them.
Cover three areas:

- Correctness and regressions: logic errors, null and empty cases, wrong state after an error, behavior that changed without the PR saying so.
- Coroutines and resources: swallowed `CancellationException`, resources not released in `finally`, I/O on the wrong dispatcher.
- Tests: production code that changed with no test, and the specific test the rules require for new `try/catch` blocks and id-derived names.

Name another area (architecture, privacy, platform) only when the diff clearly touches it.
