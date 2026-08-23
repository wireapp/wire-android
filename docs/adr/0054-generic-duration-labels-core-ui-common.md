# ADR 0054: Own generic duration labels in core UI common

**Status:** Accepted
**Baseline:** `655148d40`, `chore/android-modularization`

## Decision

Move the generic `label_off` string and the generic long-duration plurals
`seconds_long_label`, `minutes_long_label`, `hours_long_label`,
`days_long_label`, and `weeks_long_label` from `:app` resources to
`:core:ui-common`.

The move preserves every existing locale, plural quantity, and text value:
`label_off` has sixteen qualifiers and every plural has ten qualifiers.
The three duplicate Cells `label_off` definitions have the same values as
their app equivalents and are removed. Cells uses the existing
`:core:ui-common` dependency and resource alias instead.

All six current consumers use `com.wire.android.ui.common.R` only for the
transferred IDs. They retain their app or Cells resource namespaces for their
remaining module-owned resources. This adds no Gradle edge and changes no
runtime text or duration behaviour.

## Consequences

Generic duration labels now have a neutral resource owner suitable for the app,
Cells, and the later conversation extraction. `SelfDeletionDuration` remains
app-owned in this change: its seven short labels and its app `BuildConfig`
filter are intentionally out of scope.

This is a prerequisite for the later self-deleting-messages ViewModel move.
It does not move the self-deletion types, mapper, ViewModel, resources specific
to self deletion, Metro wiring, or a conversation feature source.

The focused ownership test verifies exact qualifier, quantity, and text parity,
zero app/Cells duplicates, and all six consumer resource references.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.common.GenericDurationLabelResourceOwnershipSourceTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:compileDebugKotlin \
  :features:cells:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening the slice if a resource has a conflicting text value,
a missing qualifier, an unproved consumer, or a required feature-to-feature or
app-to-core dependency.
