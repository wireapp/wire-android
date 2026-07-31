# 12. Centralized Make Command Surface for Repo Automation

Date: 2026-07-23

## Status

Accepted

## Context

Automation in the Android repository was spread across GitHub Actions workflow steps, root-level shell scripts, Python helpers, Gradle tasks, and a small `Makefile`. This made it harder to discover the correct command for local use, harder to keep CI and local workflows aligned, and easier for new scripts to accumulate without clear ownership.

The project already depends on a Unix-like build environment for Android development and CI, so `make` is available without introducing another developer tool dependency.

## Decision

We will use the root `Makefile` as the stable command surface for repository automation. The root `Makefile` imports purpose-specific fragments under `make/`, while implementation scripts live in purpose-based directories:

- `scripts/release/` for release-note and release-preparation helpers
- `scripts/qa/android-ui/` for Android UI test and deflake orchestration
- `scripts/dev/` for local developer utilities
- `scripts/signing/` for APK signing helpers

Gradle remains the owner of build logic. Make targets should call Gradle or scripts rather than reimplementing substantial logic inline.

CI workflows should call `make` targets for repo-owned commands when a target exists. Workflows should continue to own runner setup, checkout, caching, artifact upload, cloud credentials, deployment actions, and other platform orchestration.

## Consequences

**Positive:**
- Developers and CI share a clearer command vocabulary.
- Existing shell and Python helpers are grouped by purpose instead of accumulating at the root of `scripts/`.
- Workflow files contain fewer direct Gradle and script invocations.
- New automation has an obvious place to live and an obvious command surface to expose.
- No new developer dependency is introduced.

**Considerations:**
- Make targets must stay thin. Large shell blocks in Makefiles would recreate the same scripting problem in a different file.
- When adding a new script, contributors should place it in the appropriate purpose directory and expose it through `make` only when it is a useful local or CI entrypoint.
- CI-only platform setup should remain in GitHub Actions workflows or composite actions.
- Build-native behavior should still become Gradle tasks when it belongs to the build graph.
