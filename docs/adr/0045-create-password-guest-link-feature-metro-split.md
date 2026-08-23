# ADR 0045: Move the password-protected guest-link ViewModel into the conversation feature

**Status:** Accepted
**Baseline:** `22de444ef`, `chore/android-modularization`

## Decision

Move `CreatePasswordGuestLinkViewModel` and its focused test
package-preserving from `:app` to `:features:conversation`. Its pure
`CreatePasswordGuestLinkNavArgs` and `CreatePasswordGuestLinkState` are already
feature-owned contracts.

Replace the ViewModel's membership in
`ConversationDetailsManualViewModelFactoryGroup` with the dedicated
feature-owned `CreatePasswordGuestLinkManualViewModelFactoryGroup`. The feature
graph owns the composable gateways and preserves the explicit assisted-factory
method name `createPasswordGuestLinkViewModel`. The app removes only those
gateways and installs exactly one generated binding in
`AppSessionViewModelGraph`.

Keep the password-link screen, guest-link failure dialog, Navigation3 entries,
and route mapper app-owned. The screen uses app resources, theme/preview
infrastructure, Android clipboard/toast behavior, and the app-owned dialog.

## Consequences

No Gradle dependencies, resources, module edges, routes, or runtime behavior
change. The app remains the Navigation3 and Metro composition host. The feature
owns construction, password validation, and link-generation business state;
the app screen consumes the feature VM and state through the existing app-to-
feature dependency.

Boundary and source tests enforce package preservation, dedicated factory
ownership, exactly-once app installation, and unchanged Navigation3 gateway
usage.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.editguestaccess.CreatePasswordGuestLinkViewModelTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:testDevDebugUnitTest \\
  --tests com.wire.android.ui.home.conversations.details.editguestaccess.CreatePasswordGuestLinkViewModelAssemblyOwnershipSourceTest \\
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3SourceTest \\
  --tests com.wire.android.ui.home.conversations.details.ConversationDetailsNavigation3Test \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if the feature requires the app screen,
dialog, resources, Android host behavior, route/runtime type, or app dependency.
