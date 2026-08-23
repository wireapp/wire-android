# ADR 0035: Own shared participant strings in core UI common

**Status:** Accepted
**Baseline:** `a09039c3e`, `chore/android-modularization`

## Decision

Move `conversation_participant_you_label` and `temporary_user_label` from
`:app` resources to `:core:ui-common`, preserving each resource name, text,
qualifier, and default translatability.

The two strings are independent of the conversation feature: the conversation
participant renderer, the ongoing-call participant renderer, and user-profile
presentation use them. `:core:ui-common` is already the neutral UI dependency of
those consumers, so it can own the shared Android resources without a
feature-to-feature dependency or a new Gradle edge.

`ConversationParticipantItem` also uses the existing
`core:ui-common` `content_description_empty` resource. The app-specific resource
reference is therefore removed from that renderer for all three common labels.

## Consequences

The resource identifiers and localized output do not change. The app keeps
feature-specific participant strings until their consumers move with their own
feature slice. This decision does not move a renderer, alter navigation, add a
Metro binding, or make `:core:ui-common` depend on a feature.

The ownership test checks all qualifier/value pairs, proves the strings no longer
exist in `:app`, and keeps the three independent consumers on the neutral `R`
namespace.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.common.SharedParticipantStringResourceOwnershipSourceTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :core:ui-common:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if another consumer requires an app-only
resource contract, a feature resource dependency, or a new module edge.
