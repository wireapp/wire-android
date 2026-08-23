# ADR 0020: Conversation host configuration

**Status:** Accepted
**Baseline:** `2e20a150b`, `chore/android-modularization`

## Context

The conversation extraction target currently reads app-flavor `BuildConfig` values
and app-owned `FeatureVisibilityFlags`. A future move of those sources to
`:features:conversation` must not transfer flavor ownership, app implementation
details, or the existing `LocalFeatureVisibilityFlags` contract into the feature.

## Decision

`:features:conversation` owns the Android-only neutral
`ConversationHostConfiguration` interface, the
`ConversationRuntimeCapabilities` and `ConversationUiVisibility` data classes,
and a fail-fast `LocalConversationHostConfiguration` static CompositionLocal.
The feature has no BuildConfig, flavor, app, Kalium, server-flag, or Metro
dependency in this foundation.

The app owns `AppConversationHostConfiguration`, which projects exactly these
host values:

- Runtime capabilities: `IS_BUBBLE_UI_ENABLED`, `PENDING_MESSAGES`,
  `DEVELOPER_FEATURES_ENABLED`, `MLS_READ_RECEIPTS_ENABLED`, `PRIVATE_BUILD`, and
  `IS_PASSWORD_PROTECTED_GUEST_LINK_ENABLED`.
- UI visibility: `AudioMessagesIcon`, `ShareLocationIcon`, `DrawingIcon`,
  `EmojiIcon`, `GifIcon`, `PingIcon`, `ConversationSearchIcon`, and
  `SearchConversationMessages`.

The same app adapter is exposed by one `ConversationModule` provider and is
provided at `WireActivityNavigation3Host`. Existing
`LocalFeatureVisibilityFlags` remains unchanged and continues to be provided by
the host. Flavor definitions and values remain solely in `:app`, so dev, nonfree,
and fdroid retain their existing composition and `BuildConfig` ownership.
`KaliumConfigsModule` reads `pendingMessages` through the same adapter, keeping
the Kalium and conversation UI decisions on one app-owned value for every flavor.

This atomic prerequisite deliberately does not migrate a conversation, composer,
gallery, screen, or test consumer. It does not authorize resource, manifest, DI
assembly beyond the one provider, Navigation3, Metro, Kalium, server-flag, KMP, iOS,
or behavior changes.

## Enforcement and verification

`ConversationModuleBoundaryTest` verifies the configuration contract package,
rejects app/flavor/BuildConfig declarations and forbidden imports in that
contract, and fixes the six-plus-eight field budget and fail-fast static
CompositionLocal. `AppConversationHostConfigurationTest` verifies all fourteen
adapter projections against the current app sources of truth and the shared
pending-messages value used by Kalium.

Verify with JDK 21:

```sh
./gradlew :features:conversation:testDebugUnitTest --rerun-tasks
./gradlew :app:testDevDebugUnitTest --tests \
  com.wire.android.ui.home.conversations.config.AppConversationHostConfigurationTest
./gradlew :features:conversation:compileDebugKotlin :app:compileDevDebugKotlin
./gradlew :app:compileFdroidDebugKotlin
```

Stop rather than expand this slice if making an existing consumer use the contract
requires source migration, a feature flavor/BuildConfig dependency, an app-to-feature
reverse edge, a Metro/Kalium/server configuration dependency, or a change to flavor
ownership. Those are separate, explicitly reviewed follow-up slices.
