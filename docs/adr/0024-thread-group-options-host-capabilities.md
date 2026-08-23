# ADR 0024: Thread group options host capabilities

**Status:** Accepted
**Baseline:** `2b10aba8b`, `chore/android-modularization`

## Decision

Remove the remaining direct `BuildConfig` reads from the group-conversation
options renderer. The app route reads `ConversationRuntimeCapabilities` once
from `LocalConversationHostConfiguration` and passes the required values as
explicit parameters through the pure Compose renderer chain:

```text
GroupConversationDetailsRouteScreen
  -> GroupConversationDetailsContent
  -> GroupConversationOptions
  -> GroupConversationSettings
  -> conversationProtocolDetailsItems
```

The threaded values are `mlsReadReceiptsEnabled` and `privateBuild`. Public
renderer APIs do not provide host-derived defaults. Previews provide explicit
values and therefore remain independent of an app build variant.

## Dependency boundary

The host remains responsible for projecting build-flavour inputs into the
feature-owned `ConversationHostConfiguration` contract. Conversation UI reads
that contract only at its route boundary; lower-level renderers receive plain
values and stay deterministic.

This change adds no Gradle dependency. The app already depends on
`:features:conversation` for the host configuration contract. It also removes
the group-options renderer's compile-time coupling to the generated app
`BuildConfig`, which is required before the renderer can be moved to
`:features:conversation`.

## Consequences and verification

- MLS read-receipt visibility preserves the existing runtime value.
- Private-build protocol diagnostics preserve the existing runtime value.
- Preview behaviour is explicit and stable across build variants.
- No feature-to-feature edge or resource ownership change is introduced.

Verify with JDK 21:

```sh
./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.details.GroupConversationOptionsStateTest \
  --tests com.wire.android.ui.home.conversations.details.GroupDetailsViewModelTest \
  --tests com.wire.android.ui.home.conversations.config.AppConversationHostConfigurationTest
./gradlew :app:compileDevDebugKotlin :app:compileFdroidDebugKotlin
```

Future conversation renderers must receive host capabilities at their route
boundary. They must not reintroduce direct app `BuildConfig` reads or implicit
defaults in reusable UI functions.
