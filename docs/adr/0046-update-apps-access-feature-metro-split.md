# ADR 0046: Move the update-apps-access ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `bea97bd58`, `chore/android-modularization`

## Decision

Move `UpdateAppsAccessViewModel` package-preserving from `:app` to
`:features:conversation`. Its pure `UpdateAppsAccessNavArgs`,
`UpdateAppsAccessParams`, and `UpdateAppsAccessState` contracts are already
feature-owned. Keep its existing unit test in `:app`, because it uses app test
fixtures and extensions.

Replace the ViewModel's membership in
`ConversationDetailsManualViewModelFactoryGroup` with the dedicated
feature-owned `UpdateAppsAccessManualViewModelFactoryGroup`. The feature graph
owns the composable gateways and preserves the explicit assisted-factory method
name `updateAppsAccessViewModel`. The app removes only those gateways and
installs exactly one generated binding in `AppSessionViewModelGraph`.

Keep the update-apps-access screen, Navigation3 entries, route mapper, routes,
resources, and runtime flow app-owned and unchanged.

## Consequences

No Gradle dependencies, resources, module edges, route contracts, or runtime
behavior change. The app remains the Navigation3 and Metro composition host;
the feature owns the ViewModel construction and business state through the
existing app-to-feature dependency.

Boundary and source tests enforce package preservation, dedicated factory
ownership, exactly-once app installation, and unchanged Navigation3 gateway
usage.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModelTest \
  --tests com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModelAssemblyOwnershipSourceTest \
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3SourceTest \
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3Test \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the feature requires the app screen,
Navigation3 route/runtime type, resources, Android host behavior, or an app
dependency.
