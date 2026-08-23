# 17. Interaction-model Android module

Date: 2026-08-23

## Status

Accepted for the `chore/android-modularization` branch at baseline commit
`e364e0272c18842e57986ff1660885cc26f8b8cc`.

## Decision

Create Android-only `:core:interaction-model` with Android namespace
`com.wire.android.interactionmodel`. The Kotlin package remains
`com.wire.android.model`, so consumers require no source import changes.

Root `settings.gradle.kts` auto-discovers direct `core/*` directories containing a
`build.gradle.kts`; this module requires no settings change.

The following production file moves byte-identically from `:core:ui-common`:

| Old path | New path |
| --- | --- |
| `core/ui-common/src/main/kotlin/com/wire/android/model/Clickable.kt` | `core/interaction-model/src/main/kotlin/com/wire/android/model/Clickable.kt` |

The file owns `Clickable` and `ClickBlockParams`, has no imports, and uses no Android resources,
manifest declarations, DI, Kalium, Metro APIs, Navigation3, KMP, or iOS APIs. The new module has
zero production dependencies.

Direct consumers and representative evidence are:

- `:core:ui-common`: `ui/common/button/WireButton.kt` and
  `ui/common/rowitem/RowItem.kt` expose `Clickable` or `ClickBlockParams` in public composable
  parameters. Its edge is therefore `api(projects.core.interactionModel)`.
- `:app`: `app/src/main/kotlin/com/wire/android/ui/debug/conversation/DebugConversationScreen.kt`
  uses `Clickable`.
- `:core:search`: `core/search/src/main/kotlin/com/wire/android/search/users/SearchAllPeopleScreen.kt`
  uses `Clickable`.
- `:features:cells`: `features/cells/src/main/java/com/wire/android/feature/cells/ui/common/LoadingScreen.kt`
  uses `Clickable`.
- `:features:sketch`: `features/sketch/src/main/java/com/wire/android/feature/sketch/DrawingCanvasScreen.kt`
  uses `ClickBlockParams`.

The latter four edges are `implementation(projects.core.interactionModel)`. The sole `api` edge is
required because `:core:ui-common` publicly exposes both model types; reducing it to
`implementation` would break its public API boundary.

The module applies the shared Android-library and Kover conventions plus JUnit5. It explicitly
sets `android.buildFeatures.compose = false` because the module has no Compose source and must not
apply the Compose compiler plugin. The shared convention also applies Metro, so the module disables
both Metro compiler participation and automatic runtime dependency injection. Test-only dependencies
are JUnit5, Konsist, and the JUnit engine.

## Enforcement and verification

`InteractionModelArchitectureTest` scopes exactly
`core/interaction-model/src/main/kotlin`, requires a nonempty scope and the preserved
`com.wire.android.model` package, rejects production imports using Konsist semantic APIs, rejects
an edge to `:core:ui-common` with narrow build-script regexes, and requires the Compose and Metro
opt-outs.

Verification requires project discovery, the architecture test, direct-consumer compilation,
dependency insight showing no `dev.zacsweers.metro:runtime`, R100 rename/body identity checks, and
consumer scans that distinguish Wire's `com.wire.android.model.Clickable` from
`androidx.compose.ui.text.LinkAnnotation.Clickable`. Stop rather than weakening the boundary if a
production import, new consumer, `:core:interaction-model -> :core:ui-common` edge, Metro runtime,
or non-byte-identical move is found.

This ADR authorizes only this extraction. It does not authorize broader model cleanup, resources,
DI/Kalium/Navigation3 changes, KMP, or iOS work.
