# ADR 0081: Conversation media and message-search argument ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `5be88a96c`

## Context

`ConversationMediaNavArgs` and `SearchConversationMessagesNavArgs` remained in
`:app`, although both are immutable conversation contracts whose only imported type
is Kalium `ConversationId`. App-owned Navigation 3 mappers, graphs, ViewModels,
tests, and profile descriptors consume their stable public FQNs through the existing
app-to-conversation facade dependency.

The contracts carry no Android runtime, resource, navigation implementation, Metro,
or platform state. Their fields and defaults are already suitable for ownership by
the conversation facade.

## Decision

Move both argument data classes to `:features:conversation`. Preserve their package,
public FQN, source bytes, fields, defaults, and data-class behavior. Keep all callers,
Navigation 3 runtime mapping and registration, graphs, ViewModels, tests, and profile
descriptors in `:app`; the existing facade edge makes the package-preserved types
visible without caller or import changes.

No Gradle edge, resource, generated factory contract, baseline/startup profile,
stability, navigation behavior, or KMP source-set change is part of this slice. The
canonical Mermaid module graph is unchanged because repository dependency topology
is unchanged.

## Ownership boundary

The conversation facade owns the pure media and message-search argument contracts.
`:app` owns Navigation 3 runtime adaptation and registration, screens, ViewModels,
composition, resources, and profile descriptors. The preserved `com.wire.android`
packages are migration compatibility seams rather than evidence of app ownership.

Stop rather than broaden the extraction if either contract needs an app implementation
import, a caller change, a new dependency or resource, profile or stability updates,
navigation behavior changes, or KMP scaffolding.

## Consequences

The two contracts have one feature owner while every consumer continues to use the
same FQN. The source inventory changes from 181 to 179 app conversation production
files and from 90 to 92 feature production files. App conversation tests remain at
54 and feature tests remain at 31. Resource counts are unchanged.

No new focused test is needed for immutable data-only contracts. Existing
`SearchConversationMessagesViewModelTest`, Navigation 3 source tests, and module
compilation cover construction and consumption; `ConversationModuleBoundaryTest`
guards exact paths, packages, legacy app-path absence, and forbidden app imports.

## Verification

Use Java 21 to run `ConversationModuleBoundaryTest`,
`SearchConversationMessagesViewModelTest`, relevant Navigation 3 source tests, and
`ModuleDependencyBoundaryTest`. Compile the conversation feature and app dev variant,
followed by fdroid when time permits.

Confirm both source files are byte-identical, old app paths are absent, package/FQNs
and callers are unchanged, the documented source counts match, and no Gradle,
resource, profile, stability, navigation, or KMP file changed. Finish with
`git diff --check`, staged-diff review, two 100% rename-similarity entries, and a
SHA-256 digest of the normal cached patch.
