# ADR 0057: Move Edit Self-Deleting Messages ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `764422974`, `chore/android-modularization`

## Decision

Move `EditSelfDeletingMessagesViewModel`, its state, navigation arguments, and
focused unit test package-preserving from `:app` to `:features:conversation`.
The test replaces the app-only `TestConversationDetails.GROUP` fixture with a
minimal local `ConversationDetails.Group.Regular` fixture while preserving its
group behavior and assertions.

Replace this ViewModel's membership in
`ConversationDetailsManualViewModelFactoryGroup` with the feature-owned
`EditSelfDeletingMessagesManualViewModelFactoryGroup`. Its explicit generated
factory method and gateway remain `editSelfDeletingMessagesViewModel`; the app
installs the generated binding exactly once in `AppSessionViewModelGraph`.

The screen, Navigation3 route, entry, mapper, runtime behavior, resources, and
BuildConfig policy remain app-owned and unchanged. Self-deletion enum and mapper
FQNs resolve from the previously extracted `:core:ui-common` ownership.

## Consequences

There are no new Gradle dependencies, module edges, resources, routes, or
runtime changes. Boundary, assembly, and Navigation3 source tests enforce
package preservation, dedicated Metro ownership, single app installation, and
the existing app-owned Navigation3 composition.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:testDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModelTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.details.editselfdeletingmessages.EditSelfDeletingMessagesViewModelAssemblyOwnershipSourceTest \
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3SourceTest \
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3Test \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if feature ownership requires an app-only
resource, route, runtime type, Android-host behavior, or module dependency.
