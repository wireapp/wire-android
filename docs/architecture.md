# Architecture

This document describes the main runtime architecture of the Android app, the dependency direction between module families, and the boundary between this repository and the included `kalium` build.

For the repository map, see [Project Structure](./project-structure.md). For dependency inventories, see [Dependencies](./dependencies.md).

## Purpose

The architecture of `wire-android` follows a layered integration model:

- the Android application layer lives in `app/`
- reusable Android building blocks live in `core/*`
- feature-focused Android UI lives in `features/*`
- shared business logic and lower-level capabilities come from `kalium`

This repo does not implement every layer directly. Much of the domain and data-facing behavior is delegated to `kalium`, while this repo focuses on Android runtime integration and user-facing UI.

## Runtime Entry Flow

### Application Startup

The Android process starts in `WireApplication`, which is annotated with `@HiltAndroidApp`.

At startup, `WireApplication` configures and coordinates:

- Hilt-backed application dependency injection
- global logging setup
- StrictMode in debug builds
- WorkManager configuration through a custom `WireWorkerFactory`
- lifecycle observers and global observers
- sync-related background observation
- analytics observation
- call background-state observation
- asset-upload observation and worker enqueueing

This makes `WireApplication` the application-level composition root for process-wide services.

### Activity Shell

The main user-facing shell is `WireActivity`.

`WireActivity` is responsible for:

- installing and holding the splash screen until first draw
- selecting the initial destination based on app state and login mode
- setting the Compose content tree
- handling new intents and deep links
- integrating dynamic receivers and managed configurations
- providing activity-scoped coordination for top-level UI, dialogs, and navigation events

### Navigation Host

`MainNavHost` hosts the top-level Compose Destinations graph.

Its role includes:

- creating the navigation engine
- mounting the root navigation graph
- sharing selected Hilt view models across nested navigation graphs
- wiring cross-module destination dependencies
- keeping selected manual composable calls where generated wiring is not sufficient yet

This design allows feature modules to contribute destinations while the app module remains the top-level orchestration point.

## Dependency Injection

### Purpose

Dependency injection is handled with Hilt and acts as the bridge between Android framework code and `kalium` use cases or services.

### Key Components

- app-scoped Hilt modules in `app/src/main/kotlin/com/wire/android/di`
- account-scoped Hilt modules in `app/src/main/kotlin/com/wire/android/di/accountScoped`
- `WireApplication` and Android entry points such as `WireActivity`

### How It Connects

The DI layer constructs:

- application-level infrastructure such as logging, WorkManager, and configuration managers
- a shared `CoreLogic` instance from `kalium`
- session-aware and account-aware use cases derived from the current session
- feature-specific bindings consumed by view models and Android services

This means the DI layer is a key architectural seam. Android code does not manually construct most business-layer objects; it receives them from Hilt, and Hilt in turn adapts them from `kalium`.

## Module Layering

### Purpose

The repository uses module layering to keep Android concerns separated from reusable Android support code and feature-specific UI.

### Key Components

- `app/` integrates everything
- `core/*` provides reusable support code
- `features/*` provides isolated feature libraries
- `kalium` provides shared logic/data/domain capabilities

### Dependency Direction

At a high level, the dependency flow is:

- `app` depends on `core/*`, `features/*`, and `kalium`
- `features/*` depend on selected `core/*` modules and `kalium`
- `core/*` may depend on `kalium` where shared logic or data APIs are needed
- `tests/*` depend on app-facing modules for verification support

The intended architectural direction is inward toward shared logic. Android-specific code depends on reusable support layers and `kalium`, not the other way around.

## Navigation Architecture

### Purpose

Navigation is Compose-based and type-aware.

### Key Components

- Jetpack Navigation Compose
- Compose Destinations
- `core:navigation`
- app-level nav graph declarations and host wiring

### How It Connects

Navigation definitions are spread across app and feature modules, but the top-level host remains in the app module. Shared transitions, destination styles, typed routes, and shared graph-scoped view models are coordinated centrally.

This pattern allows modular feature navigation without fully decentralizing the runtime shell.

## Background Work, Sync, And Notifications

### Purpose

The app performs background observation and worker-based coordination for sync and platform events.

### Key Components

- WorkManager integration in `WireApplication`
- custom worker factory
- workers such as `AssetUploadObserverWorker`, `NotificationFetchWorker`, `PersistentWebsocketCheckWorker`, and `DeleteConversationLocallyWorker`
- notification support in `core:notification` and app notification packages
- sync-related surfaces in `features:sync` and app sync UI packages

### How It Connects

The app observes state from `kalium` and other Android services, then schedules or executes Android-native work through WorkManager and notification channels. This keeps platform execution details in the Android repo while allowing shared logic to remain in `kalium`.

## Analytics And Flavor-Dependent Behavior

### Purpose

Analytics and some integrations are flavor-dependent rather than uniform across all builds.

### Key Components

- `core:analytics`
- `core:analytics-enabled`
- `core:analytics-disabled`
- flavor configuration from `default.json`
- conditional plugin and dependency setup in Gradle

### How It Connects

The app module chooses analytics-enabled or analytics-disabled implementations per flavor. Similar flavor-aware behavior also affects Google services, developer tooling, logging, and the F-Droid build path.

This is an architectural concern because some runtime behavior is selected at build time rather than only at runtime.

## The `kalium` Boundary

### Purpose

`kalium` is an included build that provides shared logic and lower-level capabilities used by the Android app.

### Key Components

The Android build substitutes selected `com.wire.kalium:*` dependencies with local projects from the included `kalium` build, including:

- `kalium-logic`
- `kalium-util`
- `kalium-data`
- `kalium-common`
- `kalium-cells`
- selected test-facing modules

### How It Connects

From the Android repo perspective, `kalium` supplies capabilities such as:

- `CoreLogic`
- session and account handling
- messaging and calling use cases
- data and persistence-facing APIs
- network-facing abstractions
- shared domain logic for Cells and other features

The Android app does not duplicate those layers. Instead, it adapts them into Android entry points, Hilt bindings, Compose screens, notification flows, and WorkManager jobs.

This makes `kalium` an architectural boundary, not just a library dependency.

## ADRs Related To Architecture

The existing ADR set in `docs/adr/` records several architecture-affecting decisions, including:

- deep link handling refactors
- calling activity refactors
- conversation list composable refactors
- enterprise login flow support
- UI Automator adoption
- EMM config capabilities
- tablet dialog navigation parity after Compose Destinations upgrade

These ADRs are useful when the current architecture looks unusual or carries historical constraints.

## Where To Look

- Application entry point: [`../app/src/main/kotlin/com/wire/android/WireApplication.kt`](../app/src/main/kotlin/com/wire/android/WireApplication.kt)
- Main activity shell: [`../app/src/main/kotlin/com/wire/android/ui/WireActivity.kt`](../app/src/main/kotlin/com/wire/android/ui/WireActivity.kt)
- Main navigation host: [`../app/src/main/kotlin/com/wire/android/navigation/MainNavHost.kt`](../app/src/main/kotlin/com/wire/android/navigation/MainNavHost.kt)
- Manifest entry points: [`../app/src/main/AndroidManifest.xml`](../app/src/main/AndroidManifest.xml)
- Hilt modules: [`../app/src/main/kotlin/com/wire/android/di/`](../app/src/main/kotlin/com/wire/android/di/)
- Included build substitution: [`../include_builds.gradle.kts`](../include_builds.gradle.kts)
- ADRs: [`./adr/`](./adr/)
