# ADR 0044: Own conversation-details flow contracts in the conversation feature

**Status:** Accepted
**Baseline:** `8583c3f2d`, `chore/android-modularization`

## Decision

Move the following package-preserving navigation and presentation contracts from
`:app` to `:features:conversation`:

- `EditConversationNameNavArgs`
- `UpdateAppsAccessNavArgs`, `UpdateAppsAccessParams`, and `UpdateAppsAccessState`
- `EditGuestAccessNavArgs`, `EditGuestAccessParams`, and `EditGuestAccessState`
- `CreatePasswordGuestLinkNavArgs` and `CreatePasswordGuestLinkState`

These contracts describe conversation-details flows. Their Kotlin package and
public names remain unchanged, so existing app navigation mappers, screens,
ViewModels, and tests keep their source-level contracts without import or
runtime changes.

`:features:conversation` already provides the required Kalium, serialization,
Compose text-input, and core UI dependencies. The move therefore adds no
Gradle edge and does not require transferring an Android resource.

## Consequences

The feature now owns the stable arguments and immutable state used by the next
ViewModel extraction slices. `:app` remains responsible for Navigation 3
entries, screen composition, assisted ViewModel factory wiring, and runtime
side effects until each flow is extracted independently.

No Metro factory, resource namespace, route, test, or behavioural change is
part of this decision. The package-preserving move keeps review focused on file
ownership.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:testDebugUnitTest \\
  --tests com.wire.android.feature.conversation.ConversationModuleBoundaryTest \\
  --rerun-tasks
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \\
  :features:conversation:compileDebugKotlin \\
  :app:compileDevDebugKotlin \\
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if a contract requires an app-only type,
resource, runtime service, Metro factory, or a Navigation 3 change.
