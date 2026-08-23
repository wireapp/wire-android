# ADR 0029: Move message-details read-receipt projection to the conversation feature

**Status:** Accepted
**Baseline:** `2f5ddce88`, `chore/android-modularization`

## Decision

Move these production sources with their Kotlin packages, bodies, and imports unchanged:

- `app/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/model/MessageDetailsReadReceiptsData.kt`
  to `features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/model/MessageDetailsReadReceiptsData.kt`
- `app/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/usecase/ObserveReceiptsForMessageUseCase.kt`
  to `features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/usecase/ObserveReceiptsForMessageUseCase.kt`

The feature already supplies Kalium Logic, `core:ui-common`, `core:di`, coroutine
dispatchers, the participant mapper, and Metro injection support. No Gradle,
consumer, graph, resource, manifest, or behavior change is required.

The focused feature test verifies forwarding the exact receipt query, mapping every
`DetailedReceipt` through the existing participant mapper, and preserving receipt
order in `MessageDetailsReadReceiptsData`. `ConversationModuleBoundaryTest` records
the exact paths and allowed legacy-package imports.

## Consequences

This is a package-preserving R100 leaf move. It does not authorize moving the
message-details renderer or ViewModel, changing Metro, or extracting any additional
conversation code.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=100% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.messagedetails.usecase.ObserveReceiptsForMessageUseCaseTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if compilation reveals a missing feature
dependency, app implementation/resource import, Metro generation issue, or a
consumer requiring an app/feature dependency edge.
