# ADR 0042: Move the group-conversation-details ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `d2b77eb7b`, `chore/android-modularization`

## Decision

Move `GroupConversationDetailsViewModel`, its focused test, and the pure forward
`GroupConversationDetailsNavArgs` package-preserving from `:app` to
`:features:conversation`.

Keep `GroupConversationDetailsNavBackArgs` and
`GroupConversationActionType` app-owned in a separate legacy-package source.
They carry Android `Parcelable` navigation-result behavior for the app-owned
Navigation3/legacy bridge; neither type is needed to create or run the feature
ViewModel.

Replace this ViewModel's membership in
`ConversationDetailsManualViewModelFactoryGroup` with the dedicated
feature-owned `GroupConversationDetailsManualViewModelFactoryGroup`. The feature
graph owns both composable gateways and declares the explicit assisted-factory
method name `groupConversationDetailsViewModel`. The app removes only those
gateways from `ConversationDetailsViewModelGraph` and installs exactly one
generated binding container in `AppSessionViewModelGraph`.

Navigation3 remains app-owned. Routes, entry registration, the existing
route-to-forward-args mapper, result mapping, and the call to the
legacy-package gateway are retained unchanged.

## Consequences

There are no new Gradle dependencies, module edges, resource moves, route IDs,
or runtime argument changes. The pre-existing app-to-conversation dependency
provides the feature gateway to the app host, while the app remains the Metro
composition root. Separating forward args from Android result args prevents the
feature from owning an app-only back-navigation contract.

Boundary and source tests enforce package preservation, dedicated factory
ownership, the exact one-time app installation, and Navigation3's typed route
usage.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.GroupDetailsViewModelTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModelAssemblyOwnershipSourceTest \\
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3SourceTest \\
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3Test \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the feature needs an app-only route,
resource, runtime type, or a feature-to-app dependency.
