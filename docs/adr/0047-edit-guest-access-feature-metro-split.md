# ADR 0047: Move the edit-guest-access ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `f3be2b3fa`, `chore/android-modularization`

## Decision

Move `EditGuestAccessViewModel` and its focused unit test package-preserving
from `:app` to `:features:conversation`. The pure `EditGuestAccessNavArgs`,
`EditGuestAccessParams`, and `EditGuestAccessState` contracts are already
feature-owned. The test replaces only its app-only conversation-details fixture
with an equivalent local `ConversationDetails.Group.Regular` and its app-only
conversation ID with `TestConversation.ID`.

Replace the ViewModel's membership in
`ConversationDetailsManualViewModelFactoryGroup` with the dedicated
feature-owned `EditGuestAccessManualViewModelFactoryGroup`. The feature graph
owns the composable gateways and preserves the explicit assisted-factory method
name `editGuestAccessViewModel`. The app removes only those gateways and
installs exactly one generated binding in `AppSessionViewModelGraph`.

Keep the edit-guest-access screen, Navigation3 entries, route mapper, routes,
resources, and runtime flow app-owned and unchanged.

## Consequences

The feature adds the minimal test-only `junit5.params` dependency so the moved
parameterized test preserves all five existing cases. There are no production
Gradle dependencies, resources, module edges, route contracts, or runtime
behavior changes. The app remains the Navigation3 and Metro composition host;
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
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModelTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessViewModelAssemblyOwnershipSourceTest \
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3SourceTest \
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3Test \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the feature requires the app screen,
Navigation3 route/runtime type, resources, Android host behavior, or an app
dependency.
