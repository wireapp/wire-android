# ADR 0028: Move message-details reaction projection to the conversation feature

**Status:** Accepted

## Context

The `chore/android-modularization` branch at baseline `985a5d58b` still kept the
message-details reaction projection in `:app`, despite its dependencies already
being supplied by `:features:conversation`. The projection is a cohesive
conversation leaf: it observes Kalium message reactions, groups them by emoji,
maps participants through the existing conversation participant mapper, and
exposes a message-details data object.

## Decision

Move these production sources with preserved Kotlin packages, imports, and bodies:

- `app/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/model/MessageDetailsReactionsData.kt`
  to `features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/model/MessageDetailsReactionsData.kt`
- `app/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/usecase/ObserveReactionsForMessageUseCase.kt`
  to `features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/usecase/ObserveReactionsForMessageUseCase.kt`

No Gradle change is required. The feature already exposes Kalium logic and
`core:ui-common`, and already implements `core:di`; those edges provide the
Kalium use case, dispatcher, participant mapper, and existing Metro injection
assembly. The moved sources use no Android resources, app `BuildConfig`, host
configuration, Navigation, or manifest component.

The focused feature test verifies grouping equal emoji, descending ordering by
reaction count, and mapping every reaction to the matching `UIParticipant`.
`ConversationModuleBoundaryTest` records the exact moved paths and preserves the
rule that moved legacy-package sources cannot import app implementation classes,
`R`, or `BuildConfig`.

## Consequences

This is a package-preserving, R100 source move with no consumer-source or runtime
behavior change. It does not authorize related details screens, call view models,
resources, navigation, host configuration, Metro configuration, or further
conversation extraction.

## Verification and stop conditions

Run the following from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=100% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReactionsForMessageUseCaseTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening the slice if compilation reveals a missing feature
dependency, a resource or app implementation import, a Metro generation issue,
or a consumer requiring a feature-to-app/feature edge.
