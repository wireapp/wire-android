# ADR 0068: Conversation folders internal capability module

**Status:** Accepted

**Date:** 2026-08-23

**Baseline:** `40bb3ec6c`

## Context

The folder ViewModels and their dedicated Metro gateways already formed a closed,
package-preserving capability inside `:features:conversation`. Their only production
dependencies are neutral core UI/DI APIs, Kalium Logic, AndroidX/Compose, coroutines,
immutable collections, serialization, and MetroX. The app still owns folder screens,
routes, Navigation 3 runtime entries, and the session composition root.

Keeping this proved closure in the facade would leave the intended internal-module
dependency law untested. Letting app consume the child directly would instead make the
internal partition a second public feature surface.

## Decision

Create `:features:conversation:folders` as the first live internal conversation
capability. Move the following six production sources together with unchanged packages
and public FQNs:

- `ConversationFoldersVM.kt` and `ConversationFoldersViewModelGraph.kt`;
- `MoveConversationToFolderVM.kt` and `MoveConversationToFolderViewModelGraph.kt`;
- `NewFolderViewModel.kt` and `NewFolderViewModelGraph.kt`.

Move `NewFolderViewModelTest.kt` to the child. Move the three folder strings in each of
`values`, `values-de`, `values-hu`, `values-pt`, `values-ru`, and `values-si`, preserving
all 18 translations exactly. Folder code and the focused test use
`com.wire.android.feature.conversation.folders.R`.

The dependency direction is:

```text
:app -> :features:conversation -> :features:conversation:folders -> neutral core/Kalium
```

The facade uses `api` so existing app imports remain valid and `kover` so coverage is
aggregated. App keeps its single `implementationWithCoverage` facade edge and declares
no direct Gradle dependency on the child. The child cannot depend on the facade, app,
a sibling internal capability, or an external feature. Because settings discovery only
descends one level under `features`, the child is explicitly included in
`settings.gradle.kts`.

The child KSP preview aggregate is uniquely named
`ConversationFoldersViewModelScopedPreviews`. The two assisted gateways use that
aggregate. Existing Metro group FQNs, factory methods, instance keys, the direct
NewFolder map binding, and the app session binding-container installation remain
unchanged.

## Consequences

Folders is an implementation partition, not a separately supported app-facing feature.
The facade remains the only inbound conversation surface. App navigation identity and
screen/result ownership do not move. The child has no dependency budget for Parcelize,
search, Material 3, preview artifacts, datetime, Okio, or published test fixtures.

`ConversationFoldersModuleBoundaryTest` owns the folder-specific source, dependency,
resource, Metro, preview, and app-host assertions. The facade boundary test no longer
duplicates those assertions. The app module boundary permits only the exact
facade-to-folders internal edge and continues to reject all other feature-to-feature
directions.

## Verification

The change is accepted only when project discovery, parent/child clean and focused
tests, parent/child compilation, app ownership/navigation/module-boundary tests, and
the dev/fdroid app compile gates pass. Generated KSP output must show child-owned
factories and the unique two-preview aggregate, no stale folder factories or previews
in the facade, and exactly one installation of each folder binding symbol in
`AppSessionViewModelGraph`. `git diff --check` and rename detection must remain clean.
