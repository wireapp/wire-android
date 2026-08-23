# ADR 0034: Move participant presentation state into the conversation feature

**Status:** Accepted
**Baseline:** `c886872d2`, `chore/android-modularization`

## Decision

Move `GroupConversationParticipantsState` and `ParticipantsExpansionState`
package-preserving from `:app` to `:features:conversation`.

Both types are conversation presentation state: the first is the aggregate passed
to group-participant renderers and the second owns the Compose expansion state for
the members, admins, and apps sections. They depend only on the already
feature-owned participant data model, Kalium IDs/protocols, and Compose runtime.

The app retains the group-participants ViewModel, screen host, routes, and
renderers for this slice. It consumes the moved types through its existing inbound
dependency on `:features:conversation`. `MemberSectionActions` moves with
`ParticipantsExpansionState`; the app message-details receipt renderer continues
to consume the same package-preserving public type.

## Consequences

There is no navigation, ViewModel, Metro, resource, Gradle dependency, or behavior
change. The move introduces no feature-to-feature edge and leaves all package names
unchanged, so app call sites require no import edits. Keeping the small state types
with the feature-owned participant aggregate makes a later renderer move a file
relocation rather than an ownership redesign.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=100% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsStateTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.participants.GroupParticipantsViewModelTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if a moved type needs an app-only resource,
navigation contract, Metro binding, or a feature-to-app dependency.
