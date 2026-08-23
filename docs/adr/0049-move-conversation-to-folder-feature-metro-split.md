# ADR 0049: Move the move-conversation-to-folder ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `69b1ca921`, `chore/android-modularization`

## Decision

Move `MoveConversationToFolderVM` and `MoveConversationToFolderVMImpl`
package-preserving from `:app` to `:features:conversation`. Their public FQN,
state, assisted arguments, and caller-facing gateway remain unchanged.

Replace the ViewModel's membership in
`ConversationSearchFolderManualViewModelFactoryGroup` with the dedicated
feature-owned `MoveConversationToFolderManualViewModelFactoryGroup`. The
feature graph retains the `moveConversationToFolderViewModel` factory method,
implementation and interface assisted types, the
`move_conversation_to_folder_<conversationId>_<currentFolderId>` instance key,
and `previewProvider = ViewModelScopedPreviews`. That local name aliases the
feature-generated `ConversationViewModelScopedPreviews`, keeping preview
construction within the feature. The app removes only the move-to-folder
gateway and installs exactly one generated binding in
`AppSessionViewModelGraph`.

Move only `move_to_folder_success` and `move_to_folder_failed` from app
resources to feature resources. The base, German, Hungarian, Portuguese,
Russian, and Sinhala values retain their exact existing text and qualifiers;
Portuguese receives the feature's first `values-pt` resource file.

Keep the folder screen, Navigation3 entries and routes, resource callers, and
all other folder ViewModels app-owned and unchanged.

## Consequences

The moved ViewModel now imports the feature `R`; no Gradle dependencies,
module edges, route contracts, resource identifiers, or runtime behavior
change. The app remains the Navigation3 and Metro composition host, while the
feature owns assisted construction and its two user-facing move result strings.

Boundary and source tests enforce package preservation, dedicated factory
ownership, exactly-once app installation, the stable instance key and preview
provider expression, Navigation3 callers, and exact locale/resource ownership.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.EntryOwnedViewModelGatewaySourceTest \
  --tests com.wire.android.ui.home.conversations.folder.MoveConversationToFolderViewModelAssemblyOwnershipSourceTest \
  --tests com.wire.android.ui.home.conversations.ConversationAuxNavigation3SourceTest \
  --tests com.wire.android.ui.home.conversations.ConversationAuxNavigation3Test \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if feature ownership requires an app
preview aggregate, screen, Navigation3 route/runtime type, Android host
behavior, a resource outside the six audited qualifiers, or an app dependency.
