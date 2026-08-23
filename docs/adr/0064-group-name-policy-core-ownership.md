# ADR 0064: Own group-name policy in core UI common

**Status:** Accepted
**Baseline:** `6c82fdee5`, `chore/android-modularization`

## Decision

Add a resource-free `GroupNamePolicy` and `GroupNamePolicyResult` to
`:core:ui-common`, preserving the existing
`com.wire.android.ui.common.groupname` package. Keep the app-owned
`GroupNameValidator` as an adapter from the neutral result to
`GroupMetadataState`, so the existing ViewModel API and behaviour remain
unchanged.

The policy preserves the existing validation order and semantics: trim the
candidate, report empty before comparing names, reject more than 64 Kotlin
`Char` values before checking for an unchanged name, otherwise report the
trimmed candidate as unchanged or valid.

## Consequences

Group-name validation now has a neutral owner available to the conversation
feature through its existing `:core:ui-common` dependency. This adds no Gradle
edge and moves no UI component or resource.

`GroupMetadataState` remains app-owned. Its selected `Contact` values would
create a `:core:ui-common` to `:core:search` dependency cycle, while
`ChannelHistoryType` is app-owned and uses app resources and `Parcelable`.
`Contact`, `ChannelHistoryType`, and the state fields that reference them are
therefore intentionally outside this prerequisite.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:testDebugUnitTest \
  --tests com.wire.android.ui.common.groupname.GroupNamePolicyTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.common.groupname.GroupNameValidatorTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:compileDebugKotlin \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if exact validation behaviour requires
moving `GroupMetadataState`, an app resource, `Contact`, `ChannelHistoryType`,
or adding a `:core:ui-common` to `:core:search` dependency.
