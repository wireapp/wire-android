# ADR 0039: Move the all-participants screen and ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `34f9c9a87`, `chore/android-modularization`

## Decision

Move `GroupConversationAllParticipantsScreen`,
`GroupConversationAllParticipantsNavArgs`,
`GroupConversationParticipantsViewModel`, and its focused test package-preserving
from `:app` to `:features:conversation`.

Move the localized `conversation_details_participants_title` resource to the
conversation feature for `values`, `values-de`, `values-hu`, `values-ru`, and
`values-si`. The feature screen now resolves the feature resource directly.

Replace the ViewModel's membership in
`ConversationDetailsManualViewModelFactoryGroup` with the dedicated
feature-owned `GroupConversationParticipantsManualViewModelFactoryGroup`. The
feature graph owns both composable gateways and preserves the explicit assisted
factory method name `groupConversationParticipantsViewModel`. The app removes
only those two gateways from `ConversationDetailsViewModelGraph` and installs
exactly one generated binding container in `AppSessionViewModelGraph`.

Navigation3 remains app-owned: routes, entry registration, route-to-args mapper,
and the existing call to the legacy-package gateway are unchanged. The screen's
standard Compose preview moves with the screen and retains its private content
composable; only the route entry composable becomes public for the app host.

## Consequences

No module dependency, feature-to-feature edge, route ID, or runtime argument
contract changes. The feature declares the Compose preview annotation library
directly because the moved screen retains its preview. The pre-existing
app-to-conversation dependency provides the
feature implementation to Navigation3 and app previews. Feature KSP generates
the dedicated Metro factory, while the app remains the composition root.

Boundary and app source tests enforce feature ownership, exact app installation
count, unchanged Navigation3 gateway usage, and package-preserving typed args.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=100% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.participants.GroupParticipantsViewModelTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModelAssemblyOwnershipSourceTest \\
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3SourceTest \\
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3Test \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if a feature source requires an app-only
route/runtime type, preview dependency, resource, or a feature-to-app edge.
