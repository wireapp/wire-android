# ADR 0033: Move the message-details ViewModel into a dedicated feature Metro factory

**Status:** Accepted
**Baseline:** `61641807d`, `chore/android-modularization`

## Decision

Move `MessageDetailsViewModel` package-preserving from `:app` to
`:features:conversation`. Replace its membership in the shared
`ConversationCoreManualViewModelFactoryGroup` with a dedicated feature-owned
`MessageDetailsManualViewModelFactoryGroup` in `MessageDetailsViewModelGraph.kt`.

The feature graph retains both `messageDetailsViewModel` composable gateways. The
assisted factory method remains exactly `messageDetailsViewModel`, and neither
gateway supplies an instance key, preserving the prior default key semantics.

The app removes only the two message-details gateway functions from
`ConversationCoreViewModelGraph.kt`. `AppSessionViewModelGraph` remains the Metro
composition root and installs exactly one generated
`MessageDetailsManualViewModelFactoryMetroBindings` container. Media Navigation3
continues to call the same legacy-package gateway with the unchanged route args
and back callback.

## Consequences

No renderer, resource, route ID, mapper, flavor, or behavior change is made.
`:features:conversation` declares `metrox-viewmodel-compose` directly because its
generated assisted factory implements MetroX ViewModel contracts; relying on the
non-transitive `:core:di` implementation dependency would make the generated
source uncompilable. Feature KSP generates the dedicated manual factory; app Metro
graph assembly consumes its binding container. The focused feature test verifies
navigation args, both observed projection flows, and the exact Kalium receipt
type. Source boundary tests prevent the ViewModel from returning to the app
factory group.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=100% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.messagedetails.MessageDetailsViewModelTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.MessageDetailsViewModelAssemblyOwnershipSourceTest \\
  --tests com.wire.android.navigation.routes.media.MediaNavigation3SourceTest \\
  --tests com.wire.android.navigation.routes.media.MediaNavigation3Test \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if feature KSP cannot generate the dedicated
factory, app composition requires a feature-to-app edge, or route wiring changes.
