# ADR 0030: Move ExpiringMap to core UI common

**Status:** Accepted

## Context

At baseline `c3b9780da` on `chore/android-modularization`, `ExpiringMap` was
owned by `:app` even though it is a generic coroutine-backed utility. It has
independent consumers in the ongoing calling UI and in conversation Cell asset
refresh scheduling. Keeping it in app prevents the latter conversation leaf
from moving without a feature-to-app dependency.

## Decision

Move these files byte-identically while preserving their
`com.wire.android.util` package:

- `app/src/main/kotlin/com/wire/android/util/ExpiringMap.kt`
  to `core/ui-common/src/main/kotlin/com/wire/android/util/ExpiringMap.kt`
- `app/src/test/kotlin/com/wire/android/util/ExpiringMapTest.kt`
  to `core/ui-common/src/test/kotlin/com/wire/android/util/ExpiringMapTest.kt`

`:core:ui-common` is the existing neutral owner already used by app,
conversation, and calling. Its existing coroutine and JUnit5 dependencies are
sufficient. No consumer source, import, Gradle, package, API, resource,
manifest, DI, Metro, Navigation, Kalium, or KMP change is required.

## Consequences

The app and conversation callers retain their exact imports because the package
is unchanged. This is only a shared-utility prerequisite; it authorizes neither
the Cell asset refresh move nor any further conversation extraction.

## Verification and stop conditions

Run the following from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=100% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:testDebugUnitTest \
  --tests com.wire.android.util.ExpiringMapTest \
  :app:testDevDebugUnitTest \
  --tests com.wire.android.ui.home.conversations.model.messagetypes.multipart.CellAssetRefreshHelperTest \
  --tests com.wire.android.ui.calling.OngoingCallViewModelTest \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening the slice if package preservation does not retain
consumer resolution, if a dependency is missing in `:core:ui-common`, or if a
consumer compile exposes an app-to-feature dependency.
