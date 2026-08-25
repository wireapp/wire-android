# 15. Android-first modularization

Date: 2026-08-23

Updated: 2026-08-25

## Status

Accepted and active on `chore/android-modularization`. Verified against source HEAD
`0af9fc2de`.

This ADR is the canonical decision and progress record for the Android-first
modularization campaign. It consolidates and supersedes ADRs 0016–0120. Those ADRs
recorded individual extraction slices; their detailed history remains available in
Git, but new package-preserving moves do not require a new ADR. ADR 13 (shared media
player) and ADR 14 (Navigation 3) remain separate because they are independent
architectural decisions that predate this campaign.

## Context

The Android application historically owned feature presentation, navigation,
dependency assembly, resources, and Android runtime behavior in one `:app` module.
That made feature ownership unclear, encouraged accidental cross-feature coupling,
and made later Kotlin Multiplatform extraction unnecessarily expensive.

The migration is Android-first. It establishes enforceable Gradle boundaries and
platform-suitable contracts without introducing speculative KMP source sets,
`expect`/`actual` declarations, or iOS abstractions. Navigation 3 and its typed route
contracts are the foundation described by ADR 14.

## Decision

Use this dependency direction:

```text
:app -> feature facade -> internal feature capability -> neutral core / Kalium
```

The following rules are mandatory:

- `:app` is the composition root and may depend on features and neutral core modules.
- A feature must never depend on `:app` or on an unrelated feature.
- Internal capability modules are implementation partitions of their public feature
  facade; external consumers depend only on the facade.
- Code shared by at least two independent consumers moves to the smallest neutral
  core module. A common third-party dependency alone is not enough to create a
  wrapper or shared module.
- Android activities, services, providers, intents, permissions, flavor selection,
  app `BuildConfig`, Navigation 3 runtime registration, and host-only side effects
  remain in `:app`.
- Feature-owned state, ViewModels, use cases, pure mappers, presentation models,
  resources, and Metro factory contracts move with their coherent capability.
- App-owned route adapters may translate Navigation 3 arguments and install
  feature-generated Metro bindings, but they must not become an alternate owner of
  feature behavior.
- Kotlin packages and public FQNs are preserved during mechanical moves unless a
  separate API decision requires a rename.
- Resources move only with a proven owner and complete qualifier coverage; shared
  resources belong in neutral core and are not duplicated to make a module compile.
- Every module has an explicit dependency budget and boundary tests. Stop and add a
  narrow port or app adapter when a move would otherwise require a forbidden edge.

## Current module state

| Area | State | Current boundary |
|---|---|---|
| Navigation foundation | Live | `:core:navigation-kmp` owns platform-suitable route primitives; `:core:navigation` owns Android navigation helpers; `:app` owns Navigation 3 runtime composition. |
| Neutral UI foundations | Live | `:core:design-system`, `:core:query-matching`, and `:core:interaction-model` own small reusable leaves. `:core:ui-common` owns shared Android UI primitives and resources. |
| Shared media player | Live | `:core:media-player` is consumed directly by `:app` and `:features:cells`; it does not create a feature-to-feature edge. |
| Calling coordination | Live | `:core:calling` owns the neutral call coordinator and participant-count port. Conversation consumes it directly; the meetings call state is still app-hosted. |
| Authentication | Live Android feature boundary | `:features:authentication` owns authentication contracts, feature presentation/state, completion gates, resources, and tests. `:app` retains Navigation 3 entries, runtime gateways, platform effects, and host screens/adapters. The feature depends only on `:core:navigation-kmp`, `:core:ui-common`, and third-party UI/runtime libraries. |
| Conversation facade | Partially extracted | `:features:conversation` is the only conversation dependency exposed to `:app`. It owns the extracted conversation presentation/state closure and directly consumes neutral core/Kalium dependencies. |
| Conversation folders | Live internal capability | `:features:conversation:folders` is re-exported by the facade and owns folder state, ViewModels, resources, and tests. It is not consumed directly by `:app` or another feature. |
| Remaining conversation capabilities | Planned | `contract`, `participants`, `details`, `messages`, `media`, and conversation-specific `calling` modules do not exist yet. They are created only after each dependency/resource/Metro closure is proven. |
| Existing independent features | Live | `:features:cells`, `:features:sketch`, `:features:meetings`, and `:features:sync` remain independent and share code only through neutral core/Kalium modules. |

The authoritative current dependency graph is
[`../architecture/android-module-graph.md`](../architecture/android-module-graph.md).
The current-versus-target conversation topology is
[`../architecture/conversation-module-topology.md`](../architecture/conversation-module-topology.md).

## Extracted ownership to date

The campaign has established these boundaries without changing product behavior:

- dependency-light design tokens, query matching, interaction models, shared media
  playback, and calling coordination have neutral core owners;
- authentication has an Android feature owner with KMP-ready contracts and app-hosted
  runtime adapters;
- conversation participant/details state, ViewModels and Metro seams, message models,
  mappers and presentation primitives, message resources, media/search paging and
  state, call/migration/banner orchestration, guest-link/options presentation, and
  related focused tests have moved into the conversation facade;
- the folders capability has been split into the first internal conversation module;
- presentation types and resources with independent consumers have moved to
  `:core:ui-common` instead of creating feature-to-feature dependencies;
- app navigation, session composition, platform effects, services, activities, and
  runtime adapters remain deliberately app-owned.

Individual file manifests, resource counts, and temporary seams belong in boundary
tests, commit history, and the two architecture documents—not in separate ADRs.

## Remaining work

1. Continue closing the conversation facade boundary by moving coherent app-owned
   presentation leaves and their tests/resources while retaining Android host/runtime
   adapters in `:app`.
2. Break the remaining conversation/message-composer/list/gallery/calling source cycle
   through narrow neutral ports and app adapters. Do not replace it with chains between
   features.
3. Create `participants`, `details`, `messages`, `media`, `calling`, or a narrow
   `contract` internal module only when its production, resource, test, Metro, and
   navigation closure is independently buildable.
4. Move meetings call state only when it can add a direct
   `:features:meetings -> :core:calling` edge; it must never reuse conversation
   implementation.
5. Keep authentication's Android host layer narrow. A later KMP extraction may reuse
   its contracts and gateways, but this ADR does not authorize that conversion.
6. Reassess deep-link extraction only after Android intent/session/account behavior is
   separated from the existing pure resolver contracts. `:core:deeplink` remains
   deferred.

## Verification and maintenance

For each milestone:

- run the destination module's boundary and focused unit tests;
- compile the affected feature/core module and `:app` development variant;
- verify representative flavor variants when resources or flavor configuration move;
- keep package/FQN, Metro group/key/scope, route/result identity, and resource qualifier
  coverage stable;
- reject `feature -> app`, external `feature -> feature`, and `core -> feature/app`
  edges with source and Gradle boundary tests;
- update this ADR only when a module boundary, dependency law, or milestone state
  changes; ordinary file moves update tests and commit history instead;
- update both architecture diagrams in the same change as any Gradle module-edge
  change.

## Consequences

The repository has one modularization ADR instead of a new ADR per extraction slice.
The architectural law and milestone state remain reviewable, while detailed move
evidence stays close to executable boundary tests and source history.

The trade-off is that ADR 15 must be maintained as the canonical summary. A stale
status table or dependency graph is treated as an architecture defect and corrected
with the module-edge change that caused it.
