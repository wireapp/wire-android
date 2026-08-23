# ADR 0080: Conversation role projection ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `3097074ec`

## Context

`ObserveConversationRoleForUserUseCase` and `ConversationRoleData` remained in
`:app`, although they form a closed conversation-presentation projection. Their
production dependencies are Kalium conversation/user use cases and models, coroutine
`Flow`, and Metro injection. The two runtime consumers remain app-owned:
`OtherUserProfileScreenViewModel` and `ServiceDetailsViewModel`.

The projection derives the requested member role and the self member role from the
current member list, while treating a team administrator as a conversation
administrator only for a channel in that administrator's own team. Failed
conversation-details results are filtered and therefore do not emit a projection.

## Decision

Move `ObserveConversationRoleForUserUseCase` and its colocated
`ConversationRoleData` to `:features:conversation`. Preserve their package, public
FQNs, injected constructor, and behavior. Keep both profile ViewModels and their
tests in `:app`; the existing app-to-conversation facade dependency makes the
package-preserved types visible without caller changes.

Add a focused feature test using only existing feature test dependencies and
fixtures. Cover ordinary member-role projection, same-team channel team-admin
override, cross-team non-override, and failed-details non-emission.

No Gradle edge, resource, generated factory contract, baseline/startup profile,
navigation, stability, or KMP source-set change is part of this slice. The canonical
Mermaid module graph is unchanged because repository dependency topology is
unchanged.

## Ownership boundary

The conversation facade owns the role projection and its result data. `:app` owns
profile-screen orchestration, navigation, resources, and runtime composition. The
existing public package remains a migration compatibility seam rather than evidence
of app ownership.

Stop rather than broaden the extraction if the feature needs an app implementation
import, a new repository-module edge, a resource, a caller import change, a Metro
factory or profile descriptor change, navigation work, or KMP scaffolding.

## Consequences

Conversation role projection has one feature owner and focused behavioral coverage,
while both app consumers continue to use the same FQNs. The source inventory changes
from 182 to 181 app conversation production files and from 89 to 90 feature
production files; app conversation tests stay at 54 and feature tests increase from
30 to 31. Resource counts are unchanged.

## Verification

Use Java 21 to run `ObserveConversationRoleForUserUseCaseTest` with
`ConversationModuleBoundaryTest`, then app `OtherUserProfileScreenViewModelTest`,
`ServiceDetailsViewModelTest`, and `ModuleDependencyBoundaryTest`. Compile the
conversation feature and app dev variant, followed by fdroid when time permits.

Confirm the old app path is absent, package/FQNs and app callers are unchanged, the
documented source counts match, and no Gradle, resource, profile, stability,
navigation, or KMP file changed. Finish with `git diff --check`, staged-diff review,
rename-similarity inspection, and a SHA-256 digest of the staged patch.
