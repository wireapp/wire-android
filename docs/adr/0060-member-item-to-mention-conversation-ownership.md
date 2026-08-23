# ADR 0060: Move the mention-member item into the conversation feature

**Status:** Accepted
**Baseline:** `668d8ae0d`, `chore/android-modularization`

## Decision

Move `MemberItemToMention` package-preserving from `:app` to
`:features:conversation`. It is a pure Compose presentation component and
retains its public signature, preview, behavior, and existing Compose/core UI
dependencies unchanged.

## Consequences

There are no caller, import, Gradle, resource, route, or runtime changes. The
conversation boundary inventory verifies the preserved package and allows only
the existing core UI presentation dependencies required by this component.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:testDebugUnitTest \
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the component requires an app-only
dependency, resource, route, or Android-host behavior.
