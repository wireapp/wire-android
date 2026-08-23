# ADR 0023: Conversation banner member-types slice

**Status:** Accepted
**Baseline:** `2b10aba8b`, `chore/android-modularization`

## Decision

Move the package-preserving conversation-banner leaf
`ObserveConversationMembersByTypesUseCase` and its existing unit test from
`:app` to Android-only `:features:conversation`. The Kotlin package remains
`com.wire.android.ui.home.conversations.banner.usecase`, so the retained app
banner ViewModel and its Metro assembly keep their existing imports and
binding semantics.

The exact production manifest is:

- `banner/usecase/ObserveConversationMembersByTypesUseCase.kt`.

Move the matching
`banner/usecase/ObserveConversationMembersByTypesUseCaseTest.kt` unchanged.
The use case is a conversation-only projection of Kalium member details to
`UserTypeInfo`; it has no Android resource, BuildConfig, Navigation3, host
configuration, flavor, or app-service dependency.

## Dependency boundary

No Gradle edge is added. The feature already supplies Kalium Logic, coroutines,
and `DispatcherProvider` through its existing dependency budget. Its ordinary
Metro `@Inject` constructor is preserved; no app Metro graph, factory group,
or consumer moves in this slice. The app remains the runtime composition root
and consumes the package-preserved declaration via its existing feature edge.

`ConversationModuleBoundaryTest` records the exact source path and legacy
package. It continues to reject app resources, BuildConfig, and app
implementation imports, allowing only the source's required Metro `Inject`
annotation and existing feature-safe imports.

## Verification and stop conditions

Verify with JDK 21:

```sh
./gradlew :features:conversation:testDebugUnitTest --rerun-tasks \
  --tests com.wire.android.ui.home.conversations.banner.usecase.ObserveConversationMembersByTypesUseCaseTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest
./gradlew :app:testDevDebugUnitTest --rerun-tasks \
  --tests com.wire.android.ui.home.conversations.banner.ConversationBannerViewModelTest
./gradlew :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin :app:compileFdroidDebugKotlin
```

Stop rather than widening this atom if moving the leaf requires a consumer,
Metro-graph, resource, BuildConfig, or app-owned adapter change. This ADR does
not authorize any additional banner move.
