# 16. Query matching Android module

Date: 2026-08-23

## Status

Accepted for the `chore/android-modularization` branch at baseline commit
`c1bfc69a78808f701e7a8b207666c09cb373fbcc`.

## Decision

Create Android-only `:core:query-matching` with Android namespace
`com.wire.android.querymatching`. The Kotlin package remains
`com.wire.android.util` so all existing source imports remain unchanged.

The root `settings.gradle.kts` auto-discovers direct `core/*` directories that contain a
`build.gradle.kts`; it includes this module without a settings change.

The following files move byte-identically from `:core:ui-common`:

| Old path | New path |
| --- | --- |
| `core/ui-common/src/main/kotlin/com/wire/android/util/QueryMatchExtractor.kt` | `core/query-matching/src/main/kotlin/com/wire/android/util/QueryMatchExtractor.kt` |
| `core/ui-common/src/test/kotlin/com/wire/android/util/QueryMatchExtractorTest.kt` | `core/query-matching/src/test/kotlin/com/wire/android/util/QueryMatchExtractorTest.kt` |

`QueryMatchExtractor` and `MatchQueryResult` are dependency-free production declarations:
the production file has no imports and does not use Android resources, manifests, DI, Kalium,
Metro, Navigation3, KMP, or iOS APIs. `:core:ui-common` has no remaining production consumer
after the move and must not acquire an edge to this module.

The independent direct consumers are:

- `:app`, through `app/src/main/kotlin/com/wire/android/ui/markdown/MarkdownComposer.kt` and
  `app/src/main/kotlin/com/wire/android/ui/home/conversations/banner/ConversationBanner.kt`.
- `:core:search`, through
  `core/search/src/main/kotlin/com/wire/android/search/widget/HighLightName.kt` and
  `core/search/src/main/kotlin/com/wire/android/search/widget/HighLightSubtTitle.kt`.

Both edges are `implementation(projects.core.queryMatching)`; there are no `api` edges.
The module has no production dependencies. Its test-only dependencies are JUnit 5,
`kotlinx.coroutines.test`, Konsist, and the JUnit engine. The shared Android-library convention
applies Metro, so this module explicitly disables Metro compiler participation and runtime
injection with `metro.enabled = false` and `metro.automaticallyAddRuntimeDependencies = false`.

## Enforcement and verification

`QueryMatchingArchitectureTest` scopes exactly
`core/query-matching/src/main/kotlin`, requires a nonempty scope and the preserved
`com.wire.android.util` package, rejects production imports with Konsist semantic APIs, rejects
an edge to `:core:ui-common` with narrow build-script regexes, and requires both Metro opt-outs.

Verification requires Gradle project discovery, the module unit test, direct-consumer compilation,
dependency insight showing no `dev.zacsweers.metro:runtime`, rename/body identity checks, and
consumer and boundary scans. Stop rather than weakening the boundary if a production import,
additional consumer, `:core:ui-common` edge, Metro runtime dependency, or non-byte-identical move
is discovered.

This ADR authorizes only this extraction. It does not authorize a broader utility split, retained
theme/resource work, deep-link work, DI/Kalium/Navigation3 changes, KMP, or iOS work.
