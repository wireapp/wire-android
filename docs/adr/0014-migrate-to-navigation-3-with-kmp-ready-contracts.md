# 14. Migrate to Navigation 3 with KMP-ready navigation contracts

Date: 2026-08-10

## Status

Proposed

## Context

Wire currently uses Compose Destinations on top of Navigation Compose 2. Generated destinations,
route strings and `NavBackStackEntry` lookups are used not only for navigation, but also to select
Metro graphs, recover arguments and decide ViewModel ownership. This makes authentication,
multi-account switching, logout, process restoration and tablet presentation difficult to reason
about and has moved navigation recovery logic into `WireActivity`.

Navigation state, a Metro graph instance and a ViewModel owner have related, but different,
lifecycles. Treating them as one mechanism can recreate a ViewModel against the wrong account or
make an existing screen depend on a temporarily unavailable `currentSession`.

Compose Destinations is not the desired long-term navigation layer. Navigation 3 gives Wire an
application-owned, typed and saveable back stack, explicit entry decorators and a KMP-compatible
route model.

## Decision

We will replace Compose Destinations with Navigation 3 and Wire-owned navigation contracts.

- Serializable routes and navigation commands live in `core:navigation-kmp`; concrete routes stay
  with the feature that owns them.
- Features contribute typed `wireEntry` providers and expose semantic actions instead of receiving
  generated navigators or `NavHostController`.
- The Home shell aggregates one feature-owned semantic action contract for every interactive
  top-level child. Feature-specific actions are not flattened into the Home contract; roots sharing
  an action surface reuse its contract, and roots without navigation actions expose none.
- A reusable UI action contract is named after its role and omits the framework version, for example
  `ConversationListNavigationActions` or `MeetingsHomeNavigationActions`. The `Navigation3` suffix
  is reserved for contracts coupled to a Navigation 3 entry, runtime or result lifecycle, for example
  `ConversationEntryNavigation3Actions` or `MeetingsNavigation3Actions`. Starting a flow belongs to
  the framework-neutral Home-root contract; actions performed inside its entries belong to the
  `Navigation3` contract.
- Navigation 3 owns back-stack entries and their `ViewModelStoreOwner` lifecycle.
- Wire resolves the Metro graph independently from the typed route. A session route always carries
  the session identity from which its dependencies must be resolved.
- Metro creates graphs and factories, while an application-owned registry controls the identity,
  retention and disposal of graph instances.
- ViewModels receive route arguments explicitly through assisted factories. `SavedStateHandle`
  remains for restorable UI state, not as the source of navigation identity.
- Results, deep links, transitions and phone/tablet presentation use typed Wire policies.
- `WireActivity` remains responsible for Android lifecycle and platform entry points; navigation,
  session and intent decisions move to testable coordinators.

For example, route identity, graph selection and ViewModel ownership are separate:

```kotlin
@Serializable
data class ConversationRoute(
    override val sessionId: WireSessionId,
    val conversationId: ConversationId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute

wireEntry<ConversationRoute> { route ->
    ConversationScreen(
        viewModel = conversationViewModel(route.toViewModelArgs()),
        onBack = actions::back,
    )
}
```

`sessionId` selects the retained Metro session graph. Navigation 3 retains the owner identified by
the entry while that entry exists. A temporary `currentSession == null` therefore does not
reinterpret an existing route, replace its graph or recreate its ViewModel.

The migration will be delivered incrementally: KMP contracts and runtime foundation, ownership
and Metro integration, vertical destination migrations, stabilization, and removal of Compose
Destinations compatibility code.

## Consequences

Navigation becomes typed, application-owned and testable. Existing entries keep deterministic
ViewModel owners and Metro graphs during account transitions, and KMP clients can reuse route,
command and reducer contracts.

The migration temporarily increases the amount of adapter and test code. Wire must explicitly
implement and verify session transitions, results, deep links, responsive presentation and process
restoration; Navigation 3 does not provide Wire-specific policy by itself.

The migration is complete only when one production navigation runtime remains, generated
navigation references and dependencies are removed, and unit, screenshot, acceptance,
critical-flow and multi-account tests plus internal playtests are green.
