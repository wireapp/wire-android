# ADR 0053: Move the add-members ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `655148d40`, `chore/android-modularization`

## Decision

Move `AddMembersSearchNavArgs`, `AddMembersToConversationViewModel`, and its
focused unit test package-preserving from `:app` to `:features:conversation`.
Their FQNs, state, assisted arguments, and test semantics remain unchanged.

Replace the ViewModel's membership in
`ConversationSearchFolderManualViewModelFactoryGroup` with the dedicated
feature-owned `AddMembersToConversationManualViewModelFactoryGroup`. The
feature graph keeps the exact `addMembersToConversationViewModel` assisted
factory method and gateway through `wireAssistedMetroViewModel`. The app removes
only this gateway/imports and installs exactly one generated binding in
`AppSessionViewModelGraph`.

Keep `AddMembersSearchScreen`, Auxiliary Navigation3 route, entry, mapper,
runtime behavior, and resources app-owned and unchanged. In particular,
`SearchConversationMessagesViewModel` remains in its existing app-owned
legacy factory group.

## Consequences

There are no new Gradle dependencies, module edges, routes, resources, or
runtime changes. The feature owns add-members state and assisted construction,
while the app remains the Navigation3 and Metro composition host.

Boundary and source tests enforce package preservation, dedicated factory
ownership, exactly-once app installation, unchanged Auxiliary Navigation3
calling, and continued ownership of the remaining legacy search gateway.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersToConversationViewModelTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.search.adddembertoconversation.AddMembersToConversationViewModelAssemblyOwnershipSourceTest \
  --tests com.wire.android.ui.home.conversations.ConversationAuxNavigation3SourceTest \
  --tests com.wire.android.ui.home.conversations.ConversationAuxNavigation3Test \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if feature ownership requires app routes,
screens, runtime types, resources, Android-host behavior, an app dependency,
or moving broader search contracts.
