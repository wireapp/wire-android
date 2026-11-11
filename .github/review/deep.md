## Focus areas (deep review)

Cover each area below. If you can start subagents with the Task tool, start one per area in parallel and give each the PR number and its area. Merge their results, drop duplicates and apply the verification rule to every finding. If you cannot start subagents, review the areas one after another.

1. Correctness and regressions: logic errors, null and empty cases, wrong state after an error, behavior that changed without the PR saying so. Read the callers of every changed public declaration.
2. Coroutines and resources: swallowed `CancellationException`, resources not released in `finally`, I/O on the wrong dispatcher, scopes that outlive their owner, Flow collection that never stops.
3. Tests: coverage of the changed behavior, the tests the rules require for new `try/catch` blocks and id-derived names, tests that mock what a real file or database would test better, and failure paths with no assertion.
4. Architecture and boundaries: `Context` in a ViewModel, a heavier Kalium module than the code needs, JVM-only code in Kalium `commonMain`, cleanup placed outside the module that owns the data, a change that contradicts an ADR in `docs/adr/`.
5. Privacy and platform: user data left after logout, personal data in logs, calls above API 24 with no guard, main-thread work, Compose recomposition cost.

## Checks driven by the classifier reasons

Do each check that matches a reason listed below.

- Public declarations removed or changed (ABI dump or public declarations): search the app for every call site of each removed or changed declaration. Confirm each one still compiles and behaves the same.
- A `di/` file: confirm each binding is provided in the right scope and graph, and that no consumer lacks the binding.
- A `navigation/` file: confirm route arguments, back stack handling and result passing.
- A persistence file (`.sq`, `.sqm`, migrations): confirm a migration exists, and that logout and data cleanup cover every new table or file.
- A Kalium changelog fragment of type `changed`: read the fragment. Confirm the app handles the behavior change.
- A Gradle file, version catalog or ADR: confirm the change matches the decision in `docs/adr/` and does not widen a module dependency.
- A workflow or a file in `.github/review/`: confirm the permissions are the narrowest that work, untrusted text (PR title, branch name, file paths, comment bodies) never reaches a shell or an expression unquoted, and secrets and tokens are not readable by tools that take PR-controlled input.
- The Kalium bump cannot be inspected: say so under `Not verified:`.
