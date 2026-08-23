# ADR 0022: Conversation participant aggregation slice

**Status:** Accepted
**Baseline:** `c2284be55`, `chore/android-modularization`

## Decision

Move the package-preserving participant aggregation leaf from `:app` to the
Android-only `:features:conversation` module. Production and existing test
bodies remain byte-identical, and their Kotlin packages remain
`com.wire.android.*` so existing app consumers and Metro assembly preserve their
imports.

The exact production manifest is:

- `details/participants/model/ConversationParticipantsData.kt`; and
- `details/participants/usecase/ObserveParticipantsForConversationUseCase.kt`.

Move the existing `ObserveParticipantsForConversationUseCaseTest` with them.
The data contract exposes the existing feature-owned `UIParticipant` projection;
the use case is its sole producer. Together they are a coherent participant
aggregation boundary, rather than a cosmetic model move.

## Dependency boundary

No Gradle edge is added. `:features:conversation` already exposes the required
`:core:ui-common`, Kalium Logic, and coroutines APIs. The use case uses the
feature-local participant mapper and member extensions, `DispatcherProvider`
from core UI common, Kalium conversation/e2ei use cases, and an ordinary Metro
`@Inject` constructor. It has no resource, manifest, BuildConfig, host
configuration, Navigation3, KSP-preview, or manual Metro binding dependency.

The moved test uses `TestDispatcherProvider` and `testOtherUser` from
`:core:ui-common` test fixtures, plus the feature's existing JUnit5, coroutine
test, MockK, and Turbine dependencies. No fixture ownership changes.

The app remains the sole runtime composition owner. Its existing feature edge
allows the retained conversation and meetings-host consumers to use the
package-preserved declarations; the feature gains no app or feature dependency.

## Enforcement and verification

`ConversationModuleBoundaryTest` records the exact two additional production
paths and their legacy packages. It rejects app `R`, `BuildConfig`, and
non-approved app implementation imports for all moved conversation sources.

Verify with JDK 21:

```sh
./gradlew :features:conversation:testDebugUnitTest --rerun-tasks \
  --tests com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCaseTest
./gradlew :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.details.participants.GroupParticipantsViewModelTest \
  --tests com.wire.android.ui.home.meetings.MeetingsCallViewModelTest
./gradlew :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin :app:compileFdroidDebugKotlin
```

Stop rather than widening the slice if a retained app consumer needs a source
change for cross-module visibility or smart-cast semantics, if a resource or
BuildConfig dependency appears, or if Metro resolution requires an app-owned
binding/container move. This ADR authorizes no further conversation slice.
