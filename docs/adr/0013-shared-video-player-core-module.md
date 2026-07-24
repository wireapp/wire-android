# 13. Shared in-app video player as a core module

Date: 2026-07-16

## Status

Proposed

## Context

The in-app video player (`VideoPlayerScreen`, `VideoPlayerViewModel`, `VideoPlaybackState`,
`VideoViewerNavArgs`) currently lives in `features:cells` and is reachable only from the cells file
browser. It is a self-contained, reusable UI component: it takes a local path / content URL and plays
it via ExoPlayer — it holds no domain logic of its own (assets, download and offline handling already
live in Kalium and in the calling features).

We now want to play videos **in-app when a user taps a video message in a chat**. Today the chat
(which lives in the `app` module, `ui/home/conversations/…`) hands videos off to an external app via
an `ACTION_VIEW` intent. Reusing the existing player would give a consistent, in-app experience.

The player therefore needs to be consumed by two places: `features:cells` and `app` (chat). The
project's dependency law is `kalium → core → features → app`, with the additional rule that features
must not depend on other features — shared logic goes into `core:*` or Kalium. That rules out the two
shortcuts:

- Keeping it in `features:cells` and calling it from chat couples the entire "Wire Cells file storage
  and collaboration" feature to chat, and the screen is bound to the cells navigation graph via
  `@WireCellsDestination`.
- Creating a `features:video-player` module is illegal to consume from `features:cells` (feature →
  feature dependencies are forbidden).

Because the player is shared UI infrastructure used by multiple layers above `core`, its correct home
is a `core` module.

## Decision

Extract the video player into a new **`core:media-player`** module and consume it from both
`features:cells` and `app`.

A new dedicated core module is preferred over folding it into the existing `core:media` module so that
the heavy media dependencies (`media3-exoplayer`, `media3-ui`, `coil3-video`) stay scoped to modules
that actually play video, rather than leaking to everything that already depends on `core:media`
(currently only `PingRinger`). The new module can later become the home for the other in-app media
viewers (image viewer, media gallery) if we choose to consolidate them.

The two pieces that are currently cells-specific will be generalized during the move:

- **Navigation:** replace `@WireCellsDestination` with a single shared destination registered through
  `core:navigation`, so the player is navigable from any feature/app graph rather than only the cells
  graph.
- **Dependency injection:** move the Metro wiring out of `CellsViewModelFactory` so `core:media-player`
  provides its own `VideoPlayerViewModel`.

The work is sequenced extract-then-reuse, so the risky refactor is validated against the existing
caller before chat depends on it:

1. Create `core:media-player` and move the player files into it, generalizing navigation and DI. No
   user-facing behavior change.
2. Migrate `features:cells` to navigate to the shared destination and delete its copy of the player.
   This proves parity with the existing flow.
3. Wire chat in `app`: on tapping a video message, navigate to the shared destination (passing the
   already-resolved local path / content URL) instead of firing the external `ACTION_VIEW` intent.

## Consequences

- A new module `core:media-player` is added; it is auto-discovered by `settings.gradle.kts` and built
  with the `wire-android-library` convention plugin (copied from an existing core module, not from
  `features/template`). It may depend on `core:ui-common`, `core:navigation`, and `core:di` — all
  core → core, one-directional, no cycle.
- `features:cells` and `app` both depend on `core:media-player`; the `kalium → core → features → app`
  direction is preserved and no cross-feature dependency is introduced.
- The video player becomes reusable from anywhere, and chat gains an in-app video experience instead
  of delegating to an external app.
- The player's navigation destination is no longer part of the cells graph; callers navigate to the
  shared destination. Any deep links or existing navigation to the cells-scoped destination must be
  updated as part of Phase 2.
- The media3/Coil-video dependencies move to (and are scoped by) the new module.
- Follow-up opportunity (out of scope here): the cells in-app image viewer (`CellImageViewerScreen`)
  and the app `MediaGalleryScreen` overlap; `core:media-player` is a natural future home for a unified
  media viewer, to be handled as a separate ADR/change.
