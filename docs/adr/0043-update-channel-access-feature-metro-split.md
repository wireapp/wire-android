# ADR 0043: Move the channel-access ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `d5610bf53`, `chore/android-modularization`

## Decision

Move `UpdateChannelAccessViewModel` and its focused test package-preserving from
`:app` to `:features:conversation`. The feature owns a pure
`UpdateChannelAccessViewModelArgs` data class used only for assisted ViewModel
construction.

Keep the Android `Parcelable` `UpdateChannelAccessArgs` and
`ChannelAccessOnUpdateRouteScreen` in `:app`. Navigation3 remains app-owned;
its route mapper now converts the app route directly to the feature construction
arguments, while the screen continues returning the unchanged Parcelable result.

Replace the ViewModel's membership in
`ConversationDetailsManualViewModelFactoryGroup` with the dedicated
feature-owned `UpdateChannelAccessManualViewModelFactoryGroup`. The feature
graph owns the composable gateways and preserves the explicit assisted-factory
method name `updateChannelAccessViewModel`. The app removes only those gateways
and installs exactly one generated binding in `AppSessionViewModelGraph`.

## Consequences

No resources, Gradle dependencies, module edges, route contracts, or runtime
behavior change. The app remains the Navigation3 and Metro composition host;
the feature owns only construction data and business state. Boundary and source
tests guard the package-preserving move, one-time binding installation, and
typed route-to-feature-args mapping.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModelTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.updatechannelaccess.UpdateChannelAccessViewModelAssemblyOwnershipSourceTest \\
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3SourceTest \\
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3Test \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the feature requires an app-only screen,
Parcelable result type, route/runtime type, resource, or app dependency.
