# ADR 0052: Move the promote-admin ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `47ab25508`, `chore/android-modularization`

## Decision

Move `PromoteAdminViewModel`, `PromoteAdminNavArgs`, and the focused ViewModel
test package-preserving from `:app` to `:features:conversation`. The ViewModel
state, action types, assisted nav-args contract, FQNs, and test semantics stay
unchanged.

`PromoteAdminNavArgs` remains Parcelable because existing app-owned callers
and runtime routes use that contract. The feature therefore applies the
existing `BuildPlugins.kotlinParcelize` plugin and consumes the already
core-owned `QualifiedIdParceler`; it does not move broad conversation nav args
or `AssetBundle` types.

Replace membership in `ConversationSearchFolderManualViewModelFactoryGroup`
with feature-owned `PromoteAdminManualViewModelFactoryGroup`. The feature graph
keeps the exact `promoteAdminViewModel` assisted factory method and gateway via
`wireAssistedMetroViewModel`; app composition installs exactly one generated
binding in `AppSessionViewModelGraph` and removes only the old gateway/imports.

Keep `PromoteAdminScreen`, Auxiliary Navigation3 route, entry, mapper, runtime
behavior, and resources app-owned and unchanged.

## Consequences

The new Parcelize plugin is required only for the preserved Parcelable nav
args. There are no other Gradle dependencies, module edges, route contracts,
resources, or runtime behavior changes. The feature owns PromoteAdmin state
and assisted construction, while the app remains the Navigation3 and Metro
composition host.

Boundary and source tests enforce package preservation, Parcelable prerequisites,
dedicated factory ownership, exactly-once app installation, and unchanged
Navigation3 calling.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminViewModelTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.promoteadmin.PromoteAdminViewModelAssemblyOwnershipSourceTest \
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
or moving broader navigation contracts.
