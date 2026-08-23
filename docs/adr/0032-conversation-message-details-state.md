# ADR 0032: Move message-details state and navigation arguments to the conversation feature

**Status:** Accepted
**Baseline:** `4b4aab061`, `chore/android-modularization`

## Decision

Move these package-preserving production leaves from `:app` to
`:features:conversation`:

- `app/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/MessageDetailsNavArgs.kt`
  to `features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/MessageDetailsNavArgs.kt`
- `app/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/MessageDetailsState.kt`
  to `features/conversation/src/main/kotlin/com/wire/android/ui/home/conversations/messagedetails/MessageDetailsState.kt`

The files retain their bodies, imports, and Kotlin package. No test source moves
because neither leaf has a direct test. `MessageDetailsNavArgs` uses the existing
feature Kalium Logic API; `MessageDetailsState` uses the two message-details
projection models already owned by the feature.

`ConversationModuleBoundaryTest` records both exact paths and packages. Its
existing allowlist already contains the two model imports used by
`MessageDetailsState`; no additional allowlist exception is introduced.

## Consequences

This is an R100 source-only move. App Navigation3 routes, mappers, consumers,
Metro factory assembly, renderer resources, Gradle dependencies, and behavior
remain unchanged. Package preservation keeps existing app imports valid through
the existing app-to-feature dependency.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=100% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.navigation.routes.media.MediaNavigation3SourceTest \\
  --tests com.wire.android.navigation.routes.media.MediaNavigation3Test \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if package preservation fails to satisfy an
app consumer, a new feature dependency is required, or Navigation3/Metro changes
become necessary.
