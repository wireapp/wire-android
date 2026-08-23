# ADR 0026: Kalium participant-count producer for calling

**Status:** Accepted
**Baseline:** `2cdb82217`, `chore/android-modularization`

## Decision

Add `KaliumObserveConversationParticipantCount` to Android-only
`:core:calling`. It implements the existing
`ObserveConversationParticipantCount` port by observing Kalium conversation
members and mapping every emission to its list size.

The producer is public, package-preserving, and has no Metro or DI annotation.
It is deliberately constructed by its consumer from
`ObserveConversationMembersUseCase`; `:core:calling` keeps its Metro compiler
and runtime opt-outs.

## Boundary

The production source imports only Kalium Logic and coroutines, both existing
`:core:calling` API dependencies. It adds no Gradle edge and no resource,
BuildConfig, manifest, navigation, feature, app, analytics, or AVS dependency.

The existing port remains the dependency of `JoinOrStartCallManager`, preserving
its test seam. This change does not adopt the producer in app. A later app-only
adoption can give `ConversationCallViewModel` and `MeetingsCallViewModel` a
neutral member-count source, so meetings never needs a dependency on
`:features:conversation`.

## Verification and stop conditions

The unit test covers empty, non-empty, and successive Kalium member emissions.
`CallingModuleBoundaryTest` records the exact production manifest and continues
to reject Metro, app, feature, runtime, and navigation imports.

Verify with JDK 21:

```sh
./gradlew :core:calling:testDebugUnitTest --rerun-tasks
./gradlew :core:calling:compileDebugKotlin
```

Stop rather than widening this slice if the producer requires a Metro binding,
a new Gradle edge, or an app/feature dependency. This ADR does not authorize
app adoption or ViewModel migration.
