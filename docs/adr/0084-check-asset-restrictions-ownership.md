# ADR 0084: Check-asset-restrictions ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `1bf62ac32f685b7b0c0985a3d9f040458f0d294c`

## Context

`CheckAssetRestrictionsViewModel` remained in `:app` after the edit-metadata Metro
group moved to `:features:conversation`. Its public gateway was the only declaration
left in the app conversation-details graph, and its ordinary ViewModel binding remained
mixed into the broad app `WireMetroViewModelBindings` container.

The ViewModel closure also included package-stable value models in app:
`AssetBundle`, `UriAsset`, `PathParceler`, `ImportedMediaAsset`, and
`AssetTooLargeDialogState`. The ViewModel cannot move independently without leaving a
feature-to-app source dependency. These types use Android parcel APIs, Okio, Compose
stability, and Kalium attachment types already available to the conversation feature;
they require no new Gradle edge.

## Decision

Move `CheckAssetRestrictionsViewModel`, its existing focused test, `AssetBundle` and
the whole `UriAsset`/`PathParceler` file, and `ImportedMediaAsset` to
`:features:conversation` with packages and public FQNs unchanged. Extract only
`AssetTooLargeDialogState` from the app `MessageComposerViewState` file into a
feature-owned file with the same package and declaration contract.

Preserve `AssetBundle` parcelability, `@TypeParceler<Path, PathParceler>()`, the path
string encoding/decoding, fields, defaults, computed values, and attachment types.
Preserve restriction selection, state exposure, dialog fields, and hide behavior.

Replace the app conversation-details graph with a feature-owned
`CheckAssetRestrictionsViewModelGraph`. Keep the public
`checkAssetRestrictionsViewModel()` signature unchanged and add the dedicated ordinary
`CheckAssetRestrictionsMetroViewModelBindings` container using the existing
`ViewModelKey` map pattern. Remove only the old CheckAsset binding from
`WireMetroViewModelBindings`; app remains the composition root and installs the feature
container exactly once in `AppSessionViewModelGraph`.

## Ownership boundary

The conversation feature owns asset restriction state, the immutable/parcelable value
closure, the ViewModel, focused test, ordinary Metro binding, and gateway. App retains
media import orchestration, `ImportMediaAuthenticatedViewModel`, URI handling,
message drafts and sending, screens, `AssetTooLargeDialog` rendering/resources,
Navigation 3 routes/runtime, and session composition.

No Gradle/settings dependency, resource/Crowdin definition, production Navigation 3
source, baseline/startup profile, stability descriptor, KMP source, canonical Mermaid
diagram, or unrelated ViewModel/use case changes in this slice.

## Consequences

The remaining app conversation-details graph disappears, and the regular ViewModel
has one feature-owned binding installed once by app composition. App conversation
production/tests become **175/54**, conversation feature production/tests become
**101/34**, and overall app main Kotlin sources decrease from **1014** to **1010**.
Resource counts remain unchanged.

## Verification and rollback

Run the moved ViewModel and conversation boundary tests, then app assembly, media
Navigation 3, authenticated import-media, and module-boundary tests with Java 21.
Compile the feature and app dev/fdroid variants sequentially. Verify one feature
ordinary binding, no app CheckAsset binding, one session installation, old-path
absence, unchanged Parcelable source lines, exact source counts, protected-path
hashes, `git diff --check`, and rename evidence.

Rollback the ViewModel, value closure, binding/gateway, and session installation as one
unit if public FQNs, fields/defaults, Parcelable encoding, ViewModel key/scope, gateway
signature, or runtime behavior changes. Stop instead of widening the move if it needs
a new dependency, resource movement, feature-to-feature edge, profile rewrite,
Navigation 3 change, or changes to URI handling, drafts, sending, or unrelated media
ViewModels.
