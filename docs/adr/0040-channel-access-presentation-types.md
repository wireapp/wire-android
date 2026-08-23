# ADR 0040: Own channel-access presentation types in core UI common

**Status:** Accepted
**Baseline:** `c79265e52`, `chore/android-modularization`

## Decision

Move `ChannelAccessType` and `ChannelAddPermissionType` package-preserving from
`:app` to `:core:ui-common`. Move the labels used by those enum values from
`:app` resources to `:core:ui-common` for `values`, `values-de`, `values-hu`,
`values-pt`, `values-ru`, and `values-si`.

The types are shared presentation contracts: new-conversation creation uses
them, while channel details and channel-access editing consume the same values.
They do not belong to a single feature. `:core:ui-common` already has the
Kalium and Parcelable dependencies required by the existing implementation, so
the move introduces no new Gradle edge and leaves every consumer import and
runtime type unchanged.

The files retain their existing Kotlin package
`com.wire.android.ui.home.newconversation.channelaccess`. Their only source
change is that their resource lookup now uses the neutral
`com.wire.android.ui.common.R` namespace.

## Consequences

This is a prerequisite for moving conversation details state into the
conversation feature without giving that feature a dependency on `:app` or on
the new-conversation UI. The Android resource identifiers, localized text,
Parcelable contract, Kalium conversions, and call sites remain unchanged.

The ownership test proves that both types and all four label families are
owned by `:core:ui-common`, with the exact six-qualifier resource coverage and
text values preserved. It also proves the old app source files no longer
exist.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessOwnershipSourceTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :core:ui-common:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if a consumer requires an app-only
resource, a feature-specific presentation contract, or a new module edge.
