# ADR 0070: UI mention conversation ownership

**Status:** Accepted

**Date:** 2026-08-23

**Baseline:** `b1d22c9bf`

## Context

`UIMention` is the conversation composer projection of a Kalium message mention. It
defines the range, displayed handle, user identity, and conversion used when composing
conversation messages. It is not a neutral UI primitive and therefore should not
remain owned by the application composition host or move to a core module.

## Decision

Move `UIMention.kt` from `:app` to `:features:conversation` while preserving its
package, public FQN, data-class shape, conversion behavior, and every caller/import.
This is a pure ownership move.

The source may use only the conversation module's existing Kalium Logic API edge.
The move adds no Gradle dependency, resource, Metro binding, navigation contract, or
boundary-test allowlist.

Stop the extraction if it requires an app import, a new module edge, a public API
change, or a broader message-composer move. Those changes require a separate decision
and reviewable slice.

## Consequences

The mention model is owned by the conversation feature whose composition behavior it
represents. Keeping its package and source unchanged preserves existing source and
binary names while reducing app-owned conversation implementation.

## Verification

Run the focused message-composition, draft, and send-message tests, the conversation
boundary test, feature compilation, and sequential dev/fdroid app compilation with
Java 21. Verify exact source equality against the baseline, old-path absence,
`git diff --check`, and 100% rename detection.
