# ADR 0083: Edit-conversation metadata ViewModel ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `95200f6681114634ceb58dc4b24a1981b117bd31`

## Context

ADR 0082 isolated the edit-conversation-name state and validation from app-only
group-creation state. `EditConversationMetadataViewModel` and its unit test still
remained physically owned by `:app`, although their implementation closure then used
only existing feature dependencies and the package-preserved edit metadata contract.

The existing assisted Metro factory group and gateways were mixed into the app
conversation-details graph. That graph also owns the unrelated regular
`CheckAssetRestrictionsViewModel` gateway, so moving the ViewModel without separating
factory ownership would leave feature generation coupled to an app-owned group.

## Decision

Move `EditConversationMetadataViewModel` and its focused test to
`:features:conversation` while preserving the package and public FQN exactly. Preserve
the constructor, assisted `EditConversationNameNavArgs`, nested factory, public text
field/state names, initialization order, validation flow, trimmed rename behavior,
details projection, and success/failure completion semantics.

Create the dedicated feature-owned
`EditConversationMetadataManualViewModelFactoryGroup` and its zero-argument and
assisted `editConversationMetadataViewModel` gateways. Bind the moved ViewModel to
that group with the explicit factory method name. App remains the composition root and
installs the feature-generated binding exactly once in its session graph.

Remove only the metadata-assisted factory ownership from the app conversation-details
graph. Its regular `checkAssetRestrictionsViewModel` gateway remains app-owned and
continues to use the ordinary `WireMetroViewModelBindings` path. App screens,
`GroupNameScreen`, its private creation-state adapter, gateway call sites, Navigation 3
calls, resources, and runtime behavior remain unchanged.

## Compatibility and ownership boundary

This is a package/FQN/API-preserving ownership move with an annotation-only Metro
ownership delta: callers keep the same gateway names and signatures, and the ViewModel
retains
`com.wire.android.ui.home.conversations.details.metadata.EditConversationMetadataViewModel`.
The feature owns the ViewModel, dedicated Metro group/gateways, narrow state/validator,
and focused tests. App owns session composition, Navigation 3 runtime, screens,
resources, and the creation UI contract.

No Gradle edge, dependency, resource/Crowdin definition, Navigation 3 route or mapper,
KMP source, baseline/startup profile, or canonical Mermaid diagram changes in this
slice. The obsolete private assisted helper may be removed from the stability snapshot
only when compilation and generated-source evidence show that it no longer exists;
the public gateway and ViewModel descriptors remain byte-identical.

## Consequences

Edit-conversation metadata presentation now has one feature owner and a dedicated
assisted factory boundary, while app retains only runtime composition and UI
adaptation. App conversation production/tests become **178/54**, and conversation
feature production/tests become **96/33**. Resource counts remain unchanged.

## Verification and rollback

Run the moved ViewModel, edit validator, feature boundary, app assembly ownership,
Navigation 3 source, and module dependency tests with Java 21. Compile the feature and
app dev/fdroid variants sequentially. Inspect KSP output for exactly one
feature-generated assisted factory accepting `EditConversationNameNavArgs`, no app
copy or legacy group, and one session installation. Verify old path absence, source
counts, protected paths, stability descriptors, `git diff --check`, and rename
evidence.

Rollback this slice as one unit if generated Metro output deviates from the specified
dedicated factory, argument key, or single app installation, or if gateway signatures,
the ViewModel FQN, Navigation 3 behavior, or runtime behavior changes. Stop instead of
widening the move if it requires a new dependency, `GroupNameScreen` API change,
creation-state movement, resource movement, Navigation 3 changes, or another file
outside this ownership boundary.
