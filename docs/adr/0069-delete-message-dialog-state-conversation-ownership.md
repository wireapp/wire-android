# ADR 0069: Delete message dialog state conversation ownership

**Status:** Accepted

**Date:** 2026-08-23

**Baseline:** `696d95865`

## Context

`DeleteMessageDialogState` models a conversation message/media deletion action. It is
consumed by conversation message and media presentation code and is not a neutral UI
primitive. Keeping it in `:app` therefore leaves conversation-owned implementation in
the composition host.

## Decision

Move `DeleteMessageDialogState.kt` from `:app` to `:features:conversation` while
preserving its package, public FQNs, constructors, enum, and behavior. This is a pure
ownership move: callers and imports do not change.

The source may depend only on the conversation module's existing Kalium Logic edge.
The move adds no Gradle dependency, resource, Metro binding, navigation contract, or
boundary-test allowlist.

Stop the extraction if it requires an app import, a new module edge, a public API
change, or moving unrelated message/media implementation. Those changes require a
separate decision and reviewable slice.

## Consequences

Delete-message dialog state is now owned with the conversation behavior it describes,
instead of being promoted to neutral core. The package-preserving move keeps existing
source and binary names stable while reducing app-owned conversation implementation.

## Verification

Run the existing media-gallery and conversation-messages ViewModel tests, the
conversation boundary test, feature compilation, and sequential dev/fdroid app
compilation with Java 21. `git diff --check` and rename detection must remain clean.
