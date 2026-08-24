# ADR 0082: Isolate edit-conversation metadata state

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `68c80d6e3409a3e01cad8ce324dcf783d6159f7b`

## Context

`EditConversationMetadataViewModel` remains app-owned, but its public state used the
app-only `GroupMetadataState`, `GroupNameMode`, and `GroupNameValidator`. That state
also carries group-creation contacts, protocol, channel access and history, services,
and permission fields that the edit-name flow does not use. These app-only references
prevent a later package-preserving ViewModel move into `:features:conversation`.

The neutral `GroupNamePolicy` and `GroupNamePolicyResult` already belong to
`:core:ui-common`, which is an existing conversation-feature dependency. The existing
app-owned `GroupNameScreen` still consumes `GroupMetadataState` and must keep its API
and behavior while creation state and UI remain in app.

## Decision

Own `EditConversationMetadataState` and `EditGroupNameValidator` in
`:features:conversation`, preserving the edit metadata package. The state contains
only original name, animation/error flags, continue availability, channel projection,
and completion. The validator delegates to the neutral group-name policy and maps its
four results to the same flags and edit-specific errors as before.

Keep `EditConversationMetadataViewModel`, its assisted constructor and factory, Metro
binding, text field, initialization order, name observation, trimmed rename call, and
completion behavior in `:app`. Its public `editConversationState` now exposes the
narrow feature-owned state without importing app creation state or validation.

Keep `GroupNameScreen` unchanged and app-owned. `EditConversationNameRouteScreen`
privately adapts the narrow state to an edition-mode `GroupMetadataState`, including
the exact empty and too-long errors, while it handles success and failure directly
from the narrow completion state.

No ViewModel move, Metro graph change, Gradle edge, resource, Navigation 3, profile,
stability, KMP, Mermaid, or new-conversation change is part of this slice.

## Consequences

The remaining app-owned edit ViewModel is independent of app-only group-creation
state and validation, establishing a narrow seam for a later package-preserving move.
Creation state, validator adapter, screen contract, resources, and behavior remain
unchanged.

App conversation production and test counts remain **179** and **54**. Conversation
feature production and test counts become **94** and **32**. Resource counts are
unchanged.

## Verification

Run the focused feature validator and module-boundary tests, then the existing app
edit-ViewModel, creation-validator, and module-boundary tests with Java 21. Compile
`:features:conversation`, app dev, and app fdroid variants sequentially. Confirm the
app ViewModel has no imports of `GroupMetadataState`, `GroupNameMode`, or
`GroupNameValidator`; confirm the feature seam imports only the neutral core policy;
and verify the documented source counts.

Stop rather than widening this slice if preserving behavior requires changing the
`GroupNameScreen` API, moving creation state or UI, adding a dependency, or modifying
files outside this seam.
