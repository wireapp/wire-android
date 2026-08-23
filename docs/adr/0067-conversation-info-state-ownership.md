# ADR 0067: Own conversation information state in conversation

**Status:** Accepted
**Baseline:** `5f6b8e291`, `chore/android-modularization`

## Decision

Move `ConversationInfoViewState`, `ConversationDetailsData`, and
`ConversationAvatar` package-preserving from `:app` to
`:features:conversation`. Preserve their FQNs, public API, defaults, and
behaviour. Their Android presentation dependencies (`ImageAsset` and `UIText`)
are already physically owned by `:core:ui-common`; Kalium model types are
already part of the feature dependency budget.

Change one app caller to use the non-null value supplied by `let`. Kotlin does
not permit the previous repeated smart-cast of a public property after its
declaration crosses a module boundary. This is a mechanical module-boundary
adaptation and does not change the selected string or runtime behaviour.

## Consequences

Existing imports remain unchanged. No Gradle edge, resource, navigation,
Metro, or runtime-composition ownership changes. App screens and the current
`ConversationInfoViewModel` continue consuming the preserved feature contract.

The conversation boundary inventory verifies the physical feature owner and
allows only the existing neutral `ImageAsset` and `UIText` dependencies.

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

Stop rather than widening this slice if the state requires an app resource,
app implementation type, navigation contract, Metro binding, runtime adapter,
or new Gradle dependency.
