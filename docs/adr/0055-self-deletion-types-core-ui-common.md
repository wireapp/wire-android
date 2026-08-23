# ADR 0055: Own self-deletion duration types in core UI common

**Status:** Accepted
**Baseline:** `9bcfd55a8`, `chore/android-modularization`

## Decision

Move `SelfDeletionDuration` and `SelfDeletionMapper` from `:app` to
`:core:ui-common` with their existing package names and enum/mapping behaviour
unchanged. Move their seven exclusive short-label resources
(`ten_seconds_short_label`, `one_minute_short_label`,
`five_minutes_short_label`, `one_hour_short_label`, `one_day_short_label`,
`one_week_short_label`, and `four_weeks_short_label`) with every existing
localized value and qualifier. The generic long-duration labels already belong
to `:core:ui-common` under ADR 0054.

`SelfDeletionDuration.customValues` accepts the developer-feature capability as
an argument instead of reading app `BuildConfig`. The two app presentation
callers pass `BuildConfig.DEVELOPER_FEATURES_ENABLED`, preserving the existing
one-minute filtering behaviour. The type now uses only the core UI-common
resource namespace.

## Consequences

The package-preserving move leaves existing imports and mapper consumers stable
and adds no Gradle edge. It makes the neutral duration enum, mapper, and their
exclusive presentation labels available to a later conversation extraction
without pulling app configuration into core.

This is a prerequisite for the later self-deleting-messages ViewModel move. It
does not move the ViewModel, its Metro wiring, navigation, or any conversation
feature source.

Focused tests verify enum mapping, the injected capability filter, source
ownership, caller parameter flow, and exact localized resource values with no
app definitions.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:testDebugUnitTest \
  --tests com.wire.android.ui.home.messagecomposer.SelfDeletionTypesTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.common.SelfDeletionTypeOwnershipSourceTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening the slice if a short label has a conflicting value or
missing qualifier, a moved type requires an app dependency, or a new Gradle
edge is needed.
