# ADR 0050: Move the new-folder ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `41bf944d9`, `chore/android-modularization`

## Decision

Move `NewFolderViewModel` and its package-local `FolderNameState`
package-preserving from `:app` to `:features:conversation`. Move its focused
unit test package-preserving to the feature test source set. The source and
test switch only their app `R` import to the feature `R`; their behavior and
eight-test coverage remain unchanged.

Replace the app-only `ConversationSearchFolderMetroViewModelBindings` file,
which bound only this ViewModel, with the feature-owned
`NewFolderMetroViewModelBindings`. It retains the direct `@BindingContainer`,
`@ViewModelKey(NewFolderViewModel::class)` map binding and package-preserving
`newFolderViewModel() = wireMetroViewModel()` gateway. App composition installs
the replacement binding exactly once in `AppSessionViewModelGraph`.

Move only `new_folder_failure` from app resources to feature resources. The
base, German, Hungarian, Portuguese, Russian, and Sinhala values retain their
exact existing text and qualifiers. Screens, routes, Navigation3 entries, and
all other folder construction remain app-owned and unchanged.

## Consequences

There are no new Gradle dependencies, module edges, routes, resource
identifiers, or runtime changes. The feature owns direct ViewModel construction
and the failure string; the app remains the Navigation3 and Metro composition
host.

Boundary and source tests enforce package preservation, direct-binding shape,
exactly-once installation, removal of the obsolete app binding, unchanged
Navigation3 calling, and locale/resource ownership.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \
  --tests com.wire.android.ui.common.bottomsheet.folder.NewFolderViewModelTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.EntryOwnedViewModelGatewaySourceTest \
  --tests com.wire.android.ui.home.conversations.folder.NewFolderViewModelAssemblyOwnershipSourceTest \
  --tests com.wire.android.ui.home.conversations.ConversationAuxNavigation3SourceTest \
  --tests com.wire.android.ui.home.conversations.ConversationAuxNavigation3Test \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the feature requires an app resource,
screen, Navigation3 route/runtime type, Android host behavior, a resource
outside the six audited qualifiers, or an app dependency.
