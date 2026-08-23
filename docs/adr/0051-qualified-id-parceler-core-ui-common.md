# ADR 0051: Own the qualified-ID parceler in core UI common

**Status:** Accepted
**Baseline:** `41bf944d9`, `chore/android-modularization`

## Decision

Move only `QualifiedIdParceler` package-preserving from the app-owned
`ConversationNavArgs.kt` source into `:core:ui-common` at
`com.wire.android.ui.home.conversations.QualifiedIdParceler`.

`ConversationNavArgs` remains in `:app` unchanged, including its
`AssetBundle` dependency and Parcelable contract. The existing
`PromoteAdminNavArgs` and `ConversationNavArgs` `@TypeParceler` annotations
continue to resolve the same parceler FQN. The parcel encoding remains the
existing `value@domain` representation and the decoding remains unchanged.

`:core:ui-common` already applies the Parcelize plugin and has the Android and
Kalium APIs used by the parceler. This adds no Gradle edge and changes no
consumer import or runtime type.

## Consequences

The parceler is a neutral Android presentation utility used by both the
app-owned conversation navigation arguments and the Promote Admin flow. Moving
it independently avoids moving the broad `ConversationNavArgs`/`AssetBundle`
closure merely to extract Promote Admin into `:features:conversation`.

This ADR is an explicit prerequisite for the later Promote Admin feature slice.
It does not move Promote Admin, its ViewModel, its navigation route, resources,
or Metro wiring.

The focused source-ownership test proves the parceler declaration is core-owned,
the app `ConversationNavArgs` file no longer declares it, and both existing
`@TypeParceler` consumers continue to use the stable FQN.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.QualifiedIdParcelerOwnershipSourceTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :core:ui-common:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening the prerequisite if moving the parceler requires a
feature dependency, a change to the serialized format, or moving
`ConversationNavArgs` and its `AssetBundle` closure.
