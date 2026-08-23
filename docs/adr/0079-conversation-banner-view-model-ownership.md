# ADR 0079: Conversation banner ViewModel ownership

**Status:** Accepted

**Date:** 2026-08-24

**Baseline:** `3855855c560be280476f514d13495ab5065152e4`

## Context

`ConversationBannerViewModel` and its focused test remained in `:app`, although the
ViewModel owns reusable conversation presentation state and depends only on Kalium,
the feature-owned member-type observer, shared `UIText`, and localized state messages.
Its assisted input was the app-owned `ConversationNavArgs`, even though it consumed
only `conversationId`.

The banner has two distinct resource responsibilities. The ViewModel selects one of
15 state-message IDs, while the app-owned `ConversationScreen` resolves four short
span labels used by the app-owned banner renderer. Moving both sets together would
either leave feature logic using app `R` or move runtime rendering beyond the closed
ViewModel slice.

## Decision

Move `ConversationBannerViewModel` and its focused test to
`:features:conversation` with their packages and public FQNs unchanged. Replace the
assisted `ConversationNavArgs` input with `ConversationId`, retain the public
`conversationId: QualifiedID`, and bind the ViewModel to the dedicated
`ConversationBannerManualViewModelFactoryGroup` with the explicit factory method
name `conversationBannerViewModel`.

Create a feature-owned graph with a zero-argument lookup gateway and an assisted
`ConversationId` gateway. App keeps a route-facing adapter that projects only
`args.conversationId`, installs the generated feature binding once in the session
graph, and retains the unchanged Navigation 3 invocation.

Move the 15 state-message IDs into the feature's standard, Crowdin-tracked
`strings.xml` files. Default, German, Spanish, and Russian each
retain all 15 definitions; Hungarian, Italian, Polish, Portuguese, and Sinhala each
retain the existing seven non-service variants. This produces exactly 95 feature-owned
state definitions. No Swedish state definition is introduced. The four app-owned span
label IDs and their 23 localized definitions remain in app resources.

Keep `ConversationBanner`, its styling, `ConversationScreen`, and runtime rendering
in `:app`. Do not add a Gradle edge or change the canonical Mermaid graph, profiles,
stability configuration, KMP sources, media ownership, themes, or navigation routes.

## Behavioral contract

- Group conversations continue to observe member-type changes and select the same
  most-specific state message for federated, external, guest, and app/bot presence.
- One-to-one conversations do not start the member-type banner flow.
- Conversations containing only internal members continue to expose no banner.
- Initializing either group or one-to-one state still notifies Kalium that the
  conversation is open exactly once.
- The app screen continues to highlight the four short span labels with the existing
  app-owned renderer.

## Ownership boundary

The conversation facade owns the banner ViewModel, its dedicated Metro gateway,
focused unit test, and Crowdin-tracked state-message resources. App owns the route adapter, session
binding installation, screen, composable renderer, theme/runtime styling, and the four
span-label resources.

Stop rather than broaden this move if it requires a feature-to-app dependency, a new
Gradle edge, `ConversationNavArgs` or app `R` in feature code, a shared/core factory
group, duplicate Metro ownership, a changed Navigation 3 call, missing or duplicate
localized definitions, or movement of app rendering and themes.

## Consequences

Banner presentation state now has one feature owner and a narrow assisted contract.
The resource split follows the consuming code rather than the common prefix: 95 state
definitions are feature-owned, while 23 span-label definitions remain app-owned. The
review remains dominated by package-preserving moves, resource relocation, imports,
and composition wiring.

## Verification

Run the focused feature banner ViewModel/use-case tests together with
`ConversationModuleBoundaryTest`, then the app assembly, entry-owned gateway,
Navigation 3 source, and module-boundary tests. Compile the feature plus app dev and
fdroid variants sequentially with Java 21.

Inspect generated KSP output to prove the dedicated feature factory accepts
`ConversationId`, the app core generated factory has no banner method, and the session
graph installs one banner binding. Confirm the 95/23 resource split, exact qualifier
counts, the standard `strings.xml` Crowdin mapping, unchanged profiles and Navigation 3
call, old-path absence, measured source counts, `git diff --check`, and rename similarity.
