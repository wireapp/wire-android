# Project Structure

This document maps the top-level layout of the repository and explains the responsibility of each included module family.

For the repository purpose, see [General Project Description](./general-project-description.md). For runtime relationships between modules, see [Architecture](./architecture.md).

## Purpose

The repository is organized as a multi-module Gradle build centered on one Android application module plus supporting Android libraries, test modules, benchmark code, and included builds.

The authoritative module list comes from `settings.gradle.kts` and `./gradlew -q projects`.

## Top-Level Directory Map

### `app/`

Contains the Android application shell. This module defines the application entry points, manifest, flavor-aware source sets, top-level UI flows, navigation host setup, dependency injection bindings, services, notifications, and Android-specific integrations.

### `core/`

Contains reusable Android support libraries shared across the app and feature modules.

Current included modules are:

- `core:analytics`
- `core:analytics-disabled`
- `core:analytics-enabled`
- `core:di`
- `core:media`
- `core:navigation`
- `core:notification`
- `core:ui-common`
- `core:ui-common-kmp`

### `features/`

Contains feature-focused Android libraries that can be consumed by the app module.

Current included modules are:

- `features:cells`
- `features:meetings`
- `features:sketch`
- `features:sync`

The repository also contains `features/template`, which is intentionally excluded from the root build and acts as scaffolding for future feature modules.

### `tests/`

Contains Android test infrastructure.

Current included modules are:

- `tests:testsCore`
- `tests:testsSupport`

These modules support instrumentation, shared test utilities, UI automation helpers, custom runners, and secrets-loading support for test environments.

### `benchmark/`

Contains macrobenchmark-oriented code for measuring performance characteristics of the Android app.

### `wireone-kmp/`

Contains a Kotlin Multiplatform module with shared WireOne UI for Android, iOS, and browser-based Wasm targets.

### `ksp/`

Contains a small JVM/KSP helper module used for symbol-processing-related build support.

### `build-logic/`

Contains shared Gradle convention plugins and build configuration used by the root build through `includeBuild("build-logic")`.

### `kalium/`

Contains an included build that provides shared Wire logic and supporting lower-level modules. In this repository's architecture, `kalium` is primarily a boundary dependency rather than part of the Android UI layer.

### `docs/`

Contains project documentation and ADRs.

### `scripts/`

Contains repository scripts for APK wrapping, release notes, QA UI tests, and other development or CI support tasks.

## Included Gradle Projects

The root project currently includes these main Gradle projects:

- `:app`
- `:benchmark`
- `:core`
- `:features`
- `:ksp`
- `:tests`
- `:wireone-kmp`

Within those groupings, the included leaf modules are:

- `:core:analytics`
- `:core:analytics-disabled`
- `:core:analytics-enabled`
- `:core:di`
- `:core:media`
- `:core:navigation`
- `:core:notification`
- `:core:ui-common`
- `:core:ui-common-kmp`
- `:features:cells`
- `:features:meetings`
- `:features:sketch`
- `:features:sync`
- `:tests:testsCore`
- `:tests:testsSupport`

The root build also includes:

- included build `:build-logic`
- included build `:kalium`

## Responsibilities By Module Family

### App Module

### Purpose

`app/` owns the Android application process and the composition root for runtime behavior.

### Key Components

- `WireApplication`
- `WireActivity`
- Android manifest declarations
- app-scoped and account-scoped Hilt modules
- top-level navigation host setup
- Android services, broadcast receivers, notifications, and WorkManager integration

### How It Connects

`app/` depends on `core/*`, `features/*`, and multiple `kalium` artifacts. It is the integration point where Android UI and platform concerns meet shared logic from `kalium`.

### Core Modules

### Purpose

`core/*` provides shared Android libraries and reusable building blocks.

### Key Components

- `core:ui-common` for reusable UI infrastructure
- `core:navigation` for shared navigation abstractions and styles
- `core:notification` for notification-related support
- `core:media` and `core:di` for media and shared DI support
- analytics variants for enabling or disabling analytics implementation by flavor
- `core:ui-common-kmp` for limited shared UI code across Android and other targets

### How It Connects

Feature modules and the app module depend on these libraries to avoid duplicating common Android-specific code.

### Feature Modules

### Purpose

`features/*` isolates larger feature areas into separate Android libraries.

### Key Components

- `features:cells` for Cells and file-management-related UI
- `features:meetings` for meetings-related UI
- `features:sketch` for drawing/sketch interactions
- `features:sync` for sync-related UI and background coordination surfaces

### How It Connects

These modules depend on `core/*` and `kalium` APIs, then plug into the app navigation graph and app runtime shell.

### Test Modules

### Purpose

`tests/*` packages shared test support and Android instrumentation setup.

### Key Components

- custom test runner setup
- shared Android test dependencies
- UI automation support
- environment secret injection for test runs

### How It Connects

These modules support app and module-level verification rather than shipping runtime behavior.

### Included Builds

### `build-logic`

Provides convention plugins and standardized Gradle behavior used across modules.

### `kalium`

Provides shared logic/data/domain capabilities and is substituted into the Android build through `include_builds.gradle.kts`.

For more detail on the `kalium` boundary, see [Architecture](./architecture.md) and [Dependencies](./dependencies.md).

## Where To Look

- Root project inclusion logic: [`../settings.gradle.kts`](../settings.gradle.kts)
- Included build substitution: [`../include_builds.gradle.kts`](../include_builds.gradle.kts)
- App module: [`../app/`](../app/)
- Core modules: [`../core/`](../core/)
- Feature modules: [`../features/`](../features/)
- Test modules: [`../tests/`](../tests/)
- WireOne KMP module: [`../wireone-kmp/`](../wireone-kmp/)
