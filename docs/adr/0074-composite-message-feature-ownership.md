# ADR 0074: Composite message feature ownership

**Status:** Accepted

**Date:** 2026-08-23

**Baseline:** `4a0dfdafe`

## Context

`CompositeMessageArgs`, `CompositeMessageViewModel`, and their focused test remained
in `:app` even though the composite-message button state and Kalium action orchestration
belong to the conversation feature. The ViewModel was also one member of the broad
app-owned `ScopedMessageManualViewModelFactory`, while its composable gateway uses
Resaca because each lazy message item owns a keyed instance.

The existing public surface is behavior-sensitive. `CompositeMessageArgs` is
serializable and its `key` determines Resaca identity. The ViewModel prevents a second
button action while one is pending, launches the Kalium use case in `viewModelScope`,
and clears the pending button on completion. `MessageTypes` consumes the public
`compositeMessageViewModel(CompositeMessageArgs)` helper.

## Decision

Move `CompositeMessageArgs`, `CompositeMessageViewModel`,
`CompositeMessageViewModelImpl`, and `CompositeMessageViewModelTest` to
`:features:conversation` with unchanged packages and public FQNs. Preserve
serialization, `CompositeMessageArgs.ARGS_KEY`, its exact composite `key`, Resaca item
ownership, lifecycle scope, pending-state click gate, Kalium invocation, and public
helper call shape.

The feature owns a dedicated `CompositeMessageManualViewModelFactoryGroup` and the
generated `CompositeMessageManualViewModelFactoryMetroBindings`. The ViewModel is
bound to that group through `WireAssistedViewModelBinding`; the feature composable
continues to use `wireManualMetroViewModelScoped` with the feature-generated
`ConversationViewModelScopedPreviews` aggregate. It must not use the app
`ViewModelScopedPreviews` aggregate.

Remove only the composite-message method, factory argument, and binding implementation
from the app `ScopedMessageManualViewModelFactory`, `ScopedMessageViewModelGraph`, and
`WireMetroViewModelBindings`. All other scoped-message ViewModels, their factory
methods, preview providers, Resaca keys, and ownership remain unchanged. App installs
the dedicated generated binding container exactly once in `AppSessionViewModelGraph`.

## Dependency and stop conditions

This slice adds no Gradle dependency, module, resource move, navigation change,
`MessageTypes` production edit, or feature-to-feature edge. The existing conversation
edges to `:core:di`, `:core:ui-common`, and Kalium Logic are sufficient.

Stop rather than broaden the extraction if it requires app `R`, `BuildConfig`, an app
navigation import, a new Gradle edge, a change to serialization or Resaca key identity,
an app-owned assisted factory, or a preview-provider regression. Failure to generate
the dedicated factory and binding container is also a stop condition.

## Consequences

Composite-message presentation behavior, scoped arguments, tests, Resaca gateway, and
Metro factory generation now have one conversation-feature owner. App retains its
existing `MessageTypes` call site and session composition responsibility, while the
broad scoped-message factory retains only the unrelated app-owned message tools.

The canonical module graph is unchanged because this move uses existing declared
edges. The architecture document refreshes its verified baseline and source/test
counts only.

## Verification

Use Java 21 to run the moved `CompositeMessageViewModelTest`,
`ConversationModuleBoundaryTest`, `ScopedMessageManualViewModelFactoryTest`,
`EntryOwnedViewModelGatewaySourceTest`, and
`CompositeMessageViewModelAssemblyOwnershipSourceTest`. Compile
`:features:conversation`, then compile app dev and fdroid variants sequentially.

Inspect generated KSP output to prove that the feature preview aggregate contains the
composite preview, the app aggregate excludes it, the dedicated manual factory and
binding container exist, the old app manual factory contains no composite entry, and
the binding container is installed exactly once. Finish with `git diff --check`,
old-path and forbidden-import audits, and rename-similarity inspection.
