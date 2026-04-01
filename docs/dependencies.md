# Dependencies

This document describes the build stack, first-party module relationships, included-build substitution, and major third-party libraries used in the repository.

For the module map, see [Project Structure](./project-structure.md). For runtime integration details, see [Architecture](./architecture.md).

## Purpose

`wire-android` uses a mixed dependency model:

- local Gradle modules inside this repository
- included builds for shared build logic and shared Wire logic
- external Android, Kotlin, and tooling libraries
- flavor-dependent dependencies that change by distribution target

## Build System And Plugin Stack

### Purpose

The project uses Gradle Kotlin DSL with central version management and convention plugins.

### Key Components

- root `build.gradle.kts`
- root `settings.gradle.kts`
- `gradle/libs.versions.toml`
- included build `build-logic`
- standard Android, Kotlin, KSP, Compose, and CycloneDX plugins

### How It Connects

The root build declares shared plugins and repositories, while `build-logic` supplies repository-specific convention plugins such as:

- `com.wire.android.application`
- `com.wire.android.library`
- `com.wire.android.test.library`
- `com.wire.android.navigation`
- `com.wire.android.kmp.library`
- `com.wire.android.hilt`
- `com.wire.android.kover`
- `com.wire.android.versionizer`

This keeps individual module build files relatively small and consistent.

## First-Party Module Dependencies

### App Module Dependencies

`app/` depends directly on:

- `core:ui-common`
- `core:di`
- `core:media`
- `core:notification`
- `features:cells`
- `features:sketch`
- `features:meetings`
- `features:sync`
- `core:analytics-enabled` or `core:analytics-disabled`, selected by flavor
- `com.wire.kalium:*` dependencies such as `kalium-logic`, `kalium-util`, and `kalium-cells`

### Core Module Patterns

Core modules provide reusable support libraries. Examples:

- `core:navigation` depends on `core:ui-common` plus navigation libraries
- `core:notification` depends on `core:media` and selected `kalium` modules
- `core:ui-common` depends on `kalium-logic` and shared Compose/Hilt infrastructure

### Feature Module Patterns

Feature modules depend on shared UI and selected `kalium` APIs. Examples:

- `features:cells` depends on `core:ui-common`, `kalium-common`, `kalium-logic`, and `kalium-cells`
- `features:meetings` depends on `core:ui-common` and `kalium`
- `features:sync` depends on `core:notification`, `core:ui-common`, `core:di`, and `kalium`
- `features:sketch` primarily depends on `core:ui-common` and Compose/UI support

## Included Builds And Substitution

### Purpose

The repository uses included builds to keep local development aligned across shared codebases.

### Key Components

- `includeBuild("build-logic")`
- `includeBuild("kalium")`
- dependency substitution in `include_builds.gradle.kts`

### How It Connects

When the Android app depends on selected `com.wire.kalium:*` coordinates, the build substitutes them with local `kalium` projects during the included-build workflow.

This means local development can consume `kalium` source directly instead of only relying on published artifacts. The substitution currently covers modules such as:

- `com.wire.kalium:kalium-logic`
- `com.wire.kalium:kalium-util`
- `com.wire.kalium:kalium-data`
- `com.wire.kalium:kalium-common`
- `com.wire.kalium:kalium-cells`
- test-facing modules like `kalium-mocks` and `kalium-network`

From a documentation perspective, this is one of the most important dependency facts in the repo.

## Major Third-Party Libraries

### Android And Platform

- AndroidX core libraries
- AppCompat
- DataStore
- SplashScreen
- Browser
- Biometric
- Startup
- WorkManager

### UI And Navigation

- Jetpack Compose BOM and Compose UI libraries
- Material and Material3
- Navigation Compose
- Compose Destinations
- Accompanist placeholder support
- Coil for image loading

### Dependency Injection And Code Generation

- Hilt
- KSP
- JavaPoet

### Kotlin And Concurrency

- Kotlinx Coroutines
- Kotlinx Serialization
- Kotlinx Datetime
- Kotlinx Immutable Collections

### Logging, Analytics, And Reporting

- DataDog SDK integration paths
- Countly in analytics-enabled builds
- AboutLibraries for license/about screens
- CycloneDX for SBOM generation

### Testing And Quality

- JUnit 4 and JUnit 5
- MockK
- Turbine
- Robolectric
- Espresso
- AndroidX test libraries
- UI Automator
- Allure Kotlin
- Kover
- Detekt

## Flavor-Specific And Optional Dependencies

### Google Services

The root build conditionally includes the Google services Gradle plugin depending on whether the build is targeting F-Droid.

### Analytics

The app selects either analytics-enabled or analytics-disabled implementations by flavor using values loaded from `default.json`.

### FOSS Versus Non-Free Source Sets

The app module configures flavor-specific source-set additions for:

- internal versus public logging paths
- FOSS versus non-free code
- F-Droid-specific behavior and resources

This affects not only runtime features but also which dependencies and source directories participate in the build.

## Dependency Management Conventions

Centralized versions live in `gradle/libs.versions.toml`.

Most module build files use the shared version catalog and convention plugins rather than declaring fully custom dependency-management logic.

The build also generates a CycloneDX software bill of materials from the root build.

## Where To Look

- Root build: [`../build.gradle.kts`](../build.gradle.kts)
- Root settings: [`../settings.gradle.kts`](../settings.gradle.kts)
- Version catalog: [`../gradle/libs.versions.toml`](../gradle/libs.versions.toml)
- Included-build substitution: [`../include_builds.gradle.kts`](../include_builds.gradle.kts)
- App module dependencies: [`../app/build.gradle.kts`](../app/build.gradle.kts)
- Kalium overview: [`../kalium/README.md`](../kalium/README.md)
