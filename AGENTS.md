# AGENTS.md

This file provides guidance to Agents when working with code in this repository.
See https://agents.md/ for details about this file type.

## Project Overview

Wire for Android is a Jetpack Compose messaging application for the Wire platform. It handles authentication, messaging, voice/video calling, end-to-end encryption, and enterprise features. All business logic is provided by the [Kalium](kalium/AGENTS.md) Kotlin Multiplatform SDK, which is included as a git submodule.

**Requirements:** JDK 21, Android SDK, Android NDK

**First-time setup:**
```bash
git submodule update --init --recursive
# Copy local.properties to kalium/ — the IDE will NOT create it automatically
cp local.properties kalium/local.properties
```

**Key Technologies:**
- Kotlin 2.3.10 with Android Gradle Plugin 9.0.1
- Jetpack Compose + Material 3 (Compose BOM 2026.01.01)
- Hilt 2.59.1 for dependency injection
- Kotlin Coroutines 1.10.2 + Flow for async/reactive
- Coil 3 for image loading
- Compose Destinations 2.3.0 (KSP-based navigation)
- Paging 3 for paginated lists
- Kalium SDK (submodule) for all messaging, encryption, and data logic
- Detekt 1.23.8 for static analysis, Kover for code coverage

## Build Commands

```bash
# Submodule management
git submodule update --init --recursive   # First-time init
git submodule update --remote --merge     # Update to latest

# Compile and assemble
./gradlew compileApp                      # Compile only
./gradlew assembleApp                     # Assemble APK
./gradlew runApp                          # Assemble and run on connected device

# Makefile shortcuts
make assemble/staging-debug               # Assemble staging debug APK
make install/staging-debug                # Install staging debug APK

# Testing
./gradlew runUnitTests                    # All unit tests
./gradlew testCoverage                    # Unit tests + Kover XML coverage report
./gradlew runAcceptanceTests              # UI/acceptance tests (requires connected device or emulator)
./gradlew :app:testDevDebugUnitTest       # Single module unit tests (app, dev flavor)
./gradlew :<module>:test                  # Any individual module

# Code quality
./gradlew staticCodeAnalysis              # Run detektAll (must pass before finishing)

# Screenshot tests (app module only)
./gradlew :app:validateStagingDebugScreenshotTest
```

## App Flavors

Each flavor has a different icon background color for easy identification.

| Flavor    | Color  | Backend        | Logging | Description                                      |
|-----------|--------|----------------|---------|--------------------------------------------------|
| `dev`     | Red    | Wire Staging   | ✅      | Bleeding edge, for feature development           |
| `staging` | Orange | Wire Staging   | ✅      | QA testing; mirrors prod feature flags + dev tools |
| `internal`| Green  | Wire Prod      | ✅      | Internal use (limited usage)                     |
| `beta`    | Blue   | Wire Prod      | ✅      | Dogfood app for Wire employees                   |
| `prod`    | White  | Wire Prod      | ✗       | Public release on Play Store                     |
| `fdroid`  | White  | Wire Prod      | ✗       | FOSS build published on F-Droid, no closed-source code |

> **Note:** Logs are never uploaded on `prod`/`fdroid`. Sensitive content (message body, encryption keys) is never logged in any flavor.

For flavor customization details, see [CUSTOMIZATION.md](CUSTOMIZATION.md) and [default.json](default.json).

## Module Structure

Modules are auto-discovered by `settings.gradle.kts` from the `app/`, `core/`, `features/`, and `tests/` directories.

```
app/                    — Main Android application
  └── src/
      ├── main/         — Shared sources
      ├── <flavor>/     — Flavor-specific overrides (dev, staging, prod, fdroid…)
      ├── private/      — Internal flavor sources (Datadog logger)
      ├── public/       — Public flavor sources
      ├── nonfree/      — Non-FOSS sources (GMS, Firebase)
      ├── foss/         — FOSS-only sources
      ├── test/         — Unit tests
      ├── androidTest/  — Instrumentation tests
      └── screenshotTest/ — Compose screenshot tests

core/
  ├── di/               — Hilt DI modules shared across modules
  ├── ui-common/        — Shared Compose UI components, theming, and utilities
  ├── navigation/       — Navigation infrastructure and graph setup
  ├── analytics/        — Analytics tracking (enabled/disabled variants)
  ├── media/            — Audio/video media utilities
  └── notification/     — Push notification handling

features/
  ├── cells/            — Wire Cells file storage and collaboration
  ├── meetings/         — Voice/video calling UI
  ├── sync/             — Data synchronization
  ├── sketch/           — Drawing/sketch feature
  └── template/         — Starter template for new feature modules

kalium/                 — KMP messaging SDK (git submodule — see kalium/AGENTS.md)
build-logic/            — Gradle convention plugins (wire-android-application, wire-android-library, wire-hilt, etc.)
buildSrc/               — Build script helpers and legacy script plugins
tests/                  — Shared test infrastructure
benchmark/              — Benchmarking module
docs/adr/               — Architecture Decision Records
```

## Module Dependencies

The project enforces a strict layered dependency direction:

```
kalium (submodule)
    ↑
core:* (di, ui-common, navigation, analytics, media, notification)
    ↑
features:* (cells, meetings, sync, sketch)
    ↑
app
```

**Rules:**
- `core:*` modules must NOT depend on `features:*` or `app`
- `features:*` may depend on `core:*` and Kalium, but NOT on `app`
- `app` orchestrates everything and may depend on any module
- Never invert the dependency direction
- Do not add cross-feature dependencies — route shared logic through `core:*` or Kalium

## Architecture

**Pattern:** MVVM with Clean Architecture principles

**UI Layer:**
- Jetpack Compose screens annotated with `@Destination` (Compose Destinations)
- `@HiltViewModel` ViewModels injected via `hiltViewModel()` composable
- UI state exposed as `StateFlow`, one-shot effects as `SharedFlow`
- Navigation handled through `core:navigation` and Compose Destinations

**Domain / Data Layer:**
- All business logic lives in the Kalium submodule (use cases, repositories, encryption)
- ViewModels call Kalium use cases directly — no reimplementation of domain logic in this repo
- See [kalium/AGENTS.md](kalium/AGENTS.md) for the full Kalium architecture

**Data Flow:**
```
Compose UI → ViewModel (StateFlow/SharedFlow) → Kalium Use Case → Kalium Repository → Kalium Network/DB
```

**Compose Destinations:**
- Each screen file is annotated with `@Destination`
- Navigation graphs are assembled per-module and merged in `core:navigation`
- KSP generates type-safe nav argument wrappers

## Testing

**Frameworks:**
- JUnit 5 (Jupiter) with `@ParameterizedTest` support (see [ADR-0003](docs/adr/0003-introducing-junit5-parametrizable-tests.md))
- MockK for mocking (`@MockK`, `coEvery`, `every`)
- Turbine for Flow testing (`flow.test { ... }`)
- Robolectric for Android unit tests
- UI Automator for acceptance/integration tests on device (see [ADR-0007](docs/adr/0007-introducing-uiautomator.md))
- Compose screenshot testing via Android Gradle Plugin experimental support

**Test infrastructure:**
- `CoroutineTestExtension` — sets up `TestCoroutineDispatcher`; required for all ViewModel tests
  - Path: `app/src/test/kotlin/com/wire/android/config/CoroutineTestExtension.kt`
- `SnapshotExtension` — Compose `TextFieldState` snapshot testing
  - Path: `app/src/test/kotlin/com/wire/android/config/SnapshotExtension.kt`

**ViewModel test pattern — Arrangement Builder:**

```kotlin
@ExtendWith(CoroutineTestExtension::class)
class SomeViewModelTest {

    @Test
    fun givenSomeCondition_whenActionHappens_thenExpectedResult() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withSomeBehavior(Result.Success)
            .arrange()

        viewModel.onSomeAction()

        assertEquals(ExpectedState, viewModel.state.value)
    }

    private class Arrangement {
        @MockK
        lateinit var someUseCase: SomeUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withSomeBehavior(result: SomeUseCase.Result) = apply {
            coEvery { someUseCase() } returns result
        }

        fun arrange() = this to SomeViewModel(someUseCase)
    }
}
```

**Test naming convention:** `givenX_whenY_thenZ`

```kotlin
fun givenUserIsLoggedOut_whenLoginIsCalled_thenNavigateToHome()
fun givenNetworkErrorOccurs_whenSendingMessage_thenShowErrorState()
```

**Test source sets:**
- `src/test/kotlin/` — JVM unit tests (Robolectric for Android classes)
- `src/androidTest/kotlin/` — Instrumented tests (UI Automator, Espresso)
- `src/screenshotTest/kotlin/` — Compose screenshot tests

## Code Conventions

- `@HiltViewModel` with constructor injection — no manual ViewModel factories
- `StateFlow` for UI state, `SharedFlow` for one-shot navigation/side-effect events
- `@Destination` annotation on every Compose screen for type-safe navigation
- `kotlinx-datetime` (`Instant`, `LocalDateTime`) for all date/time values
- `kotlinx.serialization` with `@Serializable` for data serialization
- Do NOT expose Kalium `Either<Failure, Success>` types to the UI layer — map to concrete UI state sealed classes
- Feature flags controlled via `BuildConfig` fields set per flavor
- Flavored source sets follow the pattern: `src/<flavor>/kotlin/` for flavor-specific implementations

## Common Pitfalls

- **Missing submodule:** Always run `git submodule update --init --recursive` after cloning. Without this the build fails silently or with cryptic errors.
- **Missing `kalium/local.properties`:** The IDE does not create this file automatically. Copy it from the project root: `cp local.properties kalium/local.properties`.
- **Stale submodule reference:** After pulling, run `git submodule update --remote --merge` if the build references missing Kalium symbols.
- **`kalium.providerCacheScope` must be `GLOBAL`:** Set in `gradle.properties`. Do not change it — Kalium has no default and requires consumers to set it explicitly.
- **F-Droid flavor must stay FOSS:** Never add Google/Firebase/GMS dependencies that are reachable from the `fdroid` flavor. Use source set separation (`src/nonfree/` vs `src/foss/`).
- **Screenshot tests module opt-in:** A module must set `experimentalProperties["android.experimental.enableScreenshotTest"] = true` in its `build.gradle.kts` before screenshot tests will run.
- **Protobuf conflict:** The `app` module excludes `protobuf-java` in favor of `protobuf-lite`. Do not re-add `protobuf-java` as a dependency.
- **`libsodium.so` conflict:** Resolved via `jniLibs.pickFirsts.add("**/libsodium.so")`. Do not remove this packaging rule.

## Security Guidelines and Permissions

- Never read secrets in the codebase.
  - API keys, passwords, tokens should always be ignored and not processed.
- Allowed Without Prompting:
  - Read any source file.
  - Run linters, formatters, or type checkers on single files.
  - Run unit tests on specific test files or modules.
- Require Approval First:
  - Adding a new library/dependency.
  - Changing the dependencies between modules.
  - Git operations (`git push`, `git commit`).
  - Deleting files or directories.
  - Running full build or acceptance/E2E tests.
  - Modifying CI/CD configuration and scripts.
  - Introducing a new architectural pattern or design convention.
  - Any change that touches the `fdroid` flavor's dependency graph.

## Agent Commandments

Adhere to the following guidelines for each session:

### 1. Write code that can be tested
- If the code cannot be tested, it is not a valid solution.
- ViewModels must be testable via the Arrangement Builder pattern without an Android runtime.

### 2. All tests of changed modules must be green
- Run `./gradlew :<module>:test` for each module you modify before finishing.
- All new and modified code paths must be covered by tests.
- When fixing a bug, add a regression test.

### 3. Follow project patterns
- **ViewModels:** `@HiltViewModel`, constructor-injected use cases from Kalium, state via `StateFlow`.
- **Screens:** `@Destination`-annotated Composables, receive state from ViewModel, emit events back.
- **UI state:** Define a dedicated `sealed class` or `data class` per screen — never expose `Either` to the UI.
- **DI:** Hilt modules in `core:di`; feature-specific modules in the feature's own module.
- **New feature modules:** Copy from `features/template/` and register in `settings.gradle.kts`.

### 4. Respect module boundaries
- Dependency direction: `kalium → core → features → app`. Never invert.
- Do not add direct `app` dependencies from `core:*` or `features:*`.
- Do not create cross-feature dependencies — put shared logic in `core:*` or Kalium.
- Check existing module `build.gradle.kts` files before adding a new inter-module dependency.

### 5. Document architectural changes with an ADR
- Adding a new library/dependency or introducing a new pattern requires an ADR in `docs/adr/`.
- Name it sequentially: `docs/adr/XXXX-kebab-case-title.md` (see `0000-template-lightway-adr.md`).
- Get the ADR approved before implementing the change.

### 6. Limit scope and ask when uncertain
- Focus on narrow, well-defined tasks.
- Require approval before: adding a dependency, changing module dependencies, introducing a new pattern, touching CI/CD, deleting files, running full builds, or modifying the `fdroid` flavor's dependency graph.

### 7. Run linter before finishing
- `./gradlew staticCodeAnalysis` — must pass with zero issues on all changed files.
