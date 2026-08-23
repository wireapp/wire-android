# ADR 0048: Move the conversation-folders ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `d81031456`, `chore/android-modularization`

## Decision

Move `ConversationFoldersVM` and `ConversationFoldersVMImpl`
package-preserving from `:app` to `:features:conversation`. Their public FQN,
state, assisted arguments, and caller-facing gateway remain unchanged.

Replace the ViewModel's membership in
`ConversationSearchFolderManualViewModelFactoryGroup` with the dedicated
feature-owned `ConversationFoldersManualViewModelFactoryGroup`. The feature
graph retains the `conversationFoldersViewModel` factory method, implementation
and interface assisted types, `conversation_folders_<selectedFolderId>`
instance key, and `previewProvider = ViewModelScopedPreviews` expression. That
local name aliases the feature-generated `ConversationViewModelScopedPreviews`,
so previews remain feature-owned rather than depending on the app aggregate.
The app removes only the folders gateway and installs exactly one generated
binding in `AppSessionViewModelGraph`.

Keep `AllConversationsScreen`, the folder screen, Conversation Auxiliary
Navigation3 entries, routes, resources, and runtime flow app-owned and
unchanged.

## Consequences

The feature adds `api(libs.ktx.immutableCollections)`, because the public
`ConversationFoldersState` ABI exposes `PersistentList`. There are no other
Gradle dependencies, resources, module edges, route contracts, or runtime
behavior changes. The app remains the Navigation3 and Metro composition host;
the feature owns construction and folder-selection state through the existing
app-to-feature dependency.

Boundary and source tests enforce package preservation, dedicated factory
ownership, exactly-once app installation, the stable instance key and preview
provider expression, and unchanged callers.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.folder.ConversationFoldersViewModelAssemblyOwnershipSourceTest \
  --tests com.wire.android.ui.home.conversations.ConversationAuxNavigation3SourceTest \
  --tests com.wire.android.ui.home.conversations.ConversationAuxNavigation3Test \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the feature requires an app preview
aggregate, screen, Navigation3 route/runtime type, resources, Android host
behavior, or an app dependency.
