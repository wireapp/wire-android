# 15. Android-first modularization baseline

Date: 2026-08-23

## Status

Accepted for the `chore/android-modularization` branch, based on post-Navigation3 commit
`b7b1c2edba48dd8d02ba81de0b583671f4fa3278`.

## Baseline and notation

`A -> B` means that Gradle module `A` declares a dependency on module `B`. Active modules were
verified from the root `settings.gradle.kts` auto-discovery (`core/*` directories with a
`build.gradle.kts`) and `./gradlew projects`. Before this change,
`JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew :app:compileDevDebugKotlin` completed
with `BUILD SUCCESSFUL` (272 actionable tasks: 24 executed, 248 up-to-date).

## Deep-link ledger

`core:deeplink` is rejected/deferred on this baseline. The app-level `DeepLinkProcessor` parses
links but also handles Android `Uri`/intent behavior, session state, account switching, calls, and
`CoreLogic`; it does not meet the two-independent-consumer rule. `DeepLinkProcessor`,
`DeepLinkResult`, its constants, `UserLinkQRMapper`, and existing deeplink tests remain unchanged.

The existing pure `WireDeepLinkResolver` contracts and resolver chain remain in
`core:navigation-kmp` unchanged. A future extraction requires two independent consumers and an
explicit boundary for Android/session/account behavior; this ADR does not authorize it.

## Decision

Create Android-only `:core:design-system` (`com.wire.android.designsystem`) for dependency-light
theme tokens and types. It has proven independent consumers: `app -> core:design-system` uses
`ThemeOption` and design types, `core:navigation -> core:design-system` uses `isTablet`, and
`features:sketch -> core:design-system` uses `WireColorPalette`.

The following are mechanical, byte-identical package-preserving moves from
`core:ui-common/src/main/kotlin/com/wire/android/ui/theme/` to
`core:design-system/src/main/kotlin/com/wire/android/ui/theme/`:

| Old path | New path |
| --- | --- |
| `core/ui-common/src/main/kotlin/com/wire/android/ui/theme/AvatarColors.kt` | `core/design-system/src/main/kotlin/com/wire/android/ui/theme/AvatarColors.kt` |
| `core/ui-common/src/main/kotlin/com/wire/android/ui/theme/ThemeOption.kt` | `core/design-system/src/main/kotlin/com/wire/android/ui/theme/ThemeOption.kt` |
| `core/ui-common/src/main/kotlin/com/wire/android/ui/theme/ThemeUtils.kt` | `core/design-system/src/main/kotlin/com/wire/android/ui/theme/ThemeUtils.kt` |
| `core/ui-common/src/main/kotlin/com/wire/android/ui/theme/WireColorPalette.kt` | `core/design-system/src/main/kotlin/com/wire/android/ui/theme/WireColorPalette.kt` |
| `core/ui-common/src/main/kotlin/com/wire/android/ui/theme/WireDimensions.kt` | `core/design-system/src/main/kotlin/com/wire/android/ui/theme/WireDimensions.kt` |
| `core/ui-common/src/main/kotlin/com/wire/android/ui/theme/WireFixedColorScheme.kt` | `core/design-system/src/main/kotlin/com/wire/android/ui/theme/WireFixedColorScheme.kt` |
| `core/ui-common/src/main/kotlin/com/wire/android/ui/theme/WireTypography.kt` | `core/design-system/src/main/kotlin/com/wire/android/ui/theme/WireTypography.kt` |
| `core/ui-common/src/main/kotlin/com/wire/android/ui/theme/WireTypographyBase.kt` | `core/design-system/src/main/kotlin/com/wire/android/ui/theme/WireTypographyBase.kt` |

`Accent.kt`, `AccentSwatch.kt`, `LocalWireAccent.kt`, `StatusBarsHelper.kt`, `Theme.kt`, and
`WireColorScheme.kt` remain in `core:ui-common`: their closure includes Accent handling, strings,
snackbar/platform behavior, or resources.

`core:ui-common -> core:design-system` is the only new `api` dependency because public `WireTheme`
API exposes `WireDimensions`, `WireTypography`, and `WireFixedColorScheme`. All other new edges are
`implementation` dependencies. The new module has only Android/Compose and visibility-modifier
support; its source and runtime dependency graph has no Kalium, Metro APIs/source/runtime use, DI,
navigation, paging, Coil, serialization, appcompat, browser, WorkManager, or feature dependency.
The shared Android-library convention applies the Metro Gradle/compiler plugin. This module keeps
that convention but locally sets `metro.enabled` and
`automaticallyAddRuntimeDependencies` to `false`. The Metro plugin remains applied by the shared
convention, while compiler-plugin participation and injection of
`dev.zacsweers.metro:runtime` are disabled; no Metro API, source, or runtime dependency remains in
this module.

## Enforcement and verification

`DesignSystemArchitectureTest` uses Konsist 0.17.3, scopes exactly
`core/design-system/src/main/kotlin`, asserts that the scope is non-empty, requires every production
file's parsed package declaration to be `com.wire.android.ui.theme`, and rejects parsed imports of
Kalium, `ui-common`, DI, `CoreLogic`, Lifecycle, and Metro APIs. It also inspects this module's
`build.gradle.kts` and rejects both type-safe `projects.core.uiCommon` and string `project(...)`
edges to `:core:ui-common`. It also requires the local
`metro { enabled.set(false); automaticallyAddRuntimeDependencies.set(false) }` opt-outs that
disable convention-driven Metro compiler participation and runtime injection. Android and Compose
are intentionally allowed in this Android-only module.

The retained theme closure shares `com.wire.android.ui.theme`, so retained declarations may require
no import. Konsist does not provide a sound whole-file resolver for that case; the absent Gradle
edge plus successful compilation of `:core:design-system` and `:core:ui-common` are the
authoritative boundary checks.

Stop rather than weakening the boundary if same-package `@PackagePrivate` declarations cannot cross
the module boundary, or if Konsist 0.17.3 cannot compile/run with Kotlin 2.3.20. Verification
requires `projects`, the new-module unit test, the affected compile tasks, rename/body checks, and
forbidden-dependency scans. The next slice—deeplink extraction, retained theme closure, resources,
or any KMP/iOS work—is explicitly not authorized by this change.
