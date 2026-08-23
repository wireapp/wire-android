# ADR 0021: Conversation participant projection and typing slice

**Status:** Accepted
**Baseline:** `afdd31920`, `chore/android-modularization`

## Decision

Move the package-preserving participant projection and typing leaf from `:app` to
Android-only `:features:conversation`. Production behavior and existing unit-test
behavior are unchanged, and all six production files remain byte-identical.
The moved Kotlin packages remain legacy
`com.wire.android.*` packages so app sources and the app-owned Metro assembly keep
their existing imports.

The exact production manifest is:

- `ConversationMemberExt.kt`;
- `UIParticipantMapper.kt`;
- `details/participants/model/UIParticipant.kt`;
- `usecase/ObserveUsersTypingInConversationUseCase.kt`;
- `typing/TypingIndicatorViewModel.kt`; and
- `typing/UsersTypingViewState.kt`.

Move their three existing unit tests: `UIParticipantMapperTest`,
`ObserveUsersTypingInConversationUseCaseTest`, and
`TypingIndicatorViewModelTest`.

The mapper test previously also hosted three test factories used outside that
test. Generic `testSelfUser` and `testOtherUser` factories now live in neutral
`:core:ui-common` test fixtures. Conversation-specific `testUIParticipant` lives
in `:features:conversation` test fixtures, consumed by app tests through a
test-only app-to-feature edge. This avoids both duplication and a forbidden
core-to-feature dependency.

`UIParticipant` and its mapper form the participant projection; the use case
projects Kalium typing events, and the assisted ViewModel exposes that projection
to the conversation UI. This is one coherent leaf, not a cosmetic helper move.

## Dependency boundary

`:features:conversation` declares `api` edges to `:core:ui-common`, Kalium Logic,
AndroidX Lifecycle ViewModel, coroutines, kotlinx-datetime, and
kotlinx-serialization because the moved public surface exposes their types.
`:core:di` remains an `implementation` dependency for the source-retained
`ViewModelScopedPreview` marker. Test-only dependencies are core-ui-common test
fixtures, coroutine test, MockK, Turbine, and JUnit5.

The feature applies Kotlin serialization and KSP. KSP runs the existing preview
processor with `ConversationViewModelScopedPreviews`, a feature-specific aggregate
name which cannot collide with app's `ViewModelScopedPreviews`. The app retains
`WireMetroViewModelBindings` and `ScopedMessageViewModelGraph`; their assisted
factory and entry-owned gateway imports keep working because the moved ViewModel
and factory remain public and package-preserving. The typing gateway uses the
feature-owned preview aggregate, while all app-owned scoped ViewModels continue
to use the app aggregate.

Moving `UIParticipant` across a module boundary also requires one behavior-neutral
consumer correction: `ConversationParticipantItem` snapshots its nullable
`readReceiptDate` and `expiresAt` properties into local values before branching,
because Kotlin does not smart-cast public properties declared in another module.

No resources, manifest entries, BuildConfig values, feature flavors, host
configuration, Navigation3 routes, DI binding containers, Kalium behavior, or
Metro assembly source move in this slice.

## Enforcement and verification

`ConversationModuleBoundaryTest` verifies the exact six moved source paths and
legacy packages, rejects app `R`, `BuildConfig`, and non-approved app
implementation imports, requires the feature-specific KSP aggregate setup, and
enforces the acyclic ownership of the shared test factories.

Verify with JDK 21:

```sh
./gradlew :features:conversation:testDebugUnitTest --rerun-tasks \
  --tests com.wire.android.mapper.UIParticipantMapperTest \
  --tests com.wire.android.ui.home.conversations.usecase.ObserveUsersTypingInConversationUseCaseTest \
  --tests com.wire.android.ui.home.conversations.typing.TypingIndicatorViewModelTest
./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.di.metro.ScopedMessageManualViewModelFactoryTest
./gradlew :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin :app:compileFdroidDebugKotlin
```

Stop rather than widen the slice if a moved declaration loses public visibility,
requires app resources or BuildConfig, needs an app/feature Gradle edge, or if KSP
or Metro cannot resolve the feature-owned typing factory. The adjacent
`AssetLocalPathViewModel` remains out of scope because its implementation is
`internal` while the app's Metro assembly imports it.
