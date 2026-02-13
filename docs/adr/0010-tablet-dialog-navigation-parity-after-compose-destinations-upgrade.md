# 10. Tablet dialog navigation parity after Compose Destinations upgrade

Date: 2026-02-06

## Status

Accepted

## Context

Before the Kotlin/navigation migration (commit `18a563754`), the app used `DestinationStyle.Runtime` and `AdjustDestinationStylesForTablets()` to switch 17 destinations at runtime:

- tablet -> `DialogNavigation`
- phone -> `SlideNavigationAnimation` or `PopUpNavigationAnimation`

After the migration, generated destination styles became immutable (`val style`), so mutating `Destination.style` at runtime was no longer possible. This caused tablet screens to be shown as phone-style transitions, and not as true dialogs.

We also need to preserve:

- true dialog presentation semantics on tablet (not only animation changes)
- existing phone behavior for the same destinations
- one source of truth for the target destinations

## Decision

We introduced an app-level nav host engine wrapper and centralized route policy:

1. `WireNavHostEngine` (app module) mirrors default Compose Destinations engine behavior.
2. For each destination registration, style is resolved by `resolveTabletDialogParityStyle(...)`:
   - if tablet + route is in parity list -> force `DialogNavigation`
   - otherwise -> keep existing behavior (`manualAnimation ?: destination.style`)
3. The parity list of 17 routes is defined in `TabletDialogRoutePolicy`.
4. `TabletDialogWrapper` receives the same route matcher and applies rounded clipping for tablet parity routes as well, so visual corners match dialog expectations.

Implementation files:

- `app/src/main/kotlin/com/wire/android/navigation/WireNavHostEngine.kt`
- `app/src/main/kotlin/com/wire/android/navigation/TabletDialogRoutePolicy.kt`
- `core/navigation/src/main/kotlin/com/wire/android/navigation/wrapper/TabletDialogWrapper.kt`

Both app nav hosts use `rememberWireNavHostEngine(...)`:

- `MainNavHost`
- nested host in `HomeScreen`

## Consequences

### Positive

- Restores pre-migration tablet behavior for the 17 destinations as true dialogs.
- Preserves phone behavior without changing navigation call sites.
- Keeps route ownership centralized, making future additions/removals explicit.
- Keeps compatibility with Compose Destinations `2.3.x` immutable destination style generation.

### Trade-offs

- We maintain a small custom engine implementation that mirrors upstream behavior and should be kept in sync when upgrading Compose Destinations.
- Wrapper clipping now depends on an injected route matcher, which is another integration point to keep aligned with route policy.

### Maintenance rules

When adding/removing tablet dialog parity screens:

1. Update `TabletDialogRoutePolicy.destinationBaseRoutes`.
2. Add/update tests in `TabletDialogRoutePolicyTest`.
3. Verify both tablet and phone behavior manually.

When upgrading Compose Destinations:

1. Diff upstream default engine behavior against `WireNavHostEngine`.
2. Re-validate style resolution and manual animation precedence.

