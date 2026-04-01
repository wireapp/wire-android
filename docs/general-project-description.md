# General Project Description

This document describes what the `wire-android` repository contains, how it is positioned within the Wire codebase, and which major technical and product concerns shape the project.

For the module map, see [Project Structure](./project-structure.md). For runtime behavior, see [Architecture](./architecture.md).

## Purpose

`wire-android` is the Android client repository for Wire. It contains the Android application, Android support modules, test modules, benchmark code, and a small Kotlin Multiplatform surface used for shared UI experiments and companion targets.

The repository is not a standalone monolith. It depends heavily on an included build named `kalium`, which provides much of the domain, data, and logic layer consumed by the Android app.

## What The Repository Includes

The root Gradle build currently includes these main projects:

- `:app`
- `:benchmark`
- `:core:*`
- `:features:*`
- `:ksp`
- `:tests:*`
- `:wireone-kmp`

The root build also includes two separate builds:

- `build-logic` for convention plugins and shared Gradle setup
- `kalium` for shared Wire logic and supporting modules

For a module-by-module breakdown, see [Project Structure](./project-structure.md).

## Build Targets And Outputs

The primary build target is the Android application in `app/`.

The repository also contains:

- Android libraries in `core/` and `features/`
- Android test infrastructure in `tests/`
- Macrobenchmark code in `benchmark/`
- a Kotlin Multiplatform module in `wireone-kmp/` that targets Android, iOS, and WebAssembly browser builds
- a small `ksp/` helper module for symbol-processing-related build support

## App Flavors And Distribution Modes

The app is built in multiple flavors defined by configuration in `default.json` and Gradle source-set setup in `app/build.gradle.kts`.

Documented flavors include:

- `dev`
- `staging`
- `internal`
- `beta`
- `prod`
- `fdroid`

At a high level, the flavors separate development, QA, internal dogfooding, production, and F-Droid distribution use cases. They also change behavior such as logging, analytics, backend defaults, developer tooling, and inclusion of non-free integrations.

The `fdroid` flavor is the FOSS-oriented production variant and excludes non-free pieces that are present in other flavors.

## Major Technologies

The repository is primarily built with:

- Kotlin
- Gradle Kotlin DSL
- Android Gradle Plugin
- Jetpack Compose for UI
- Hilt for dependency injection
- WorkManager for background work
- Compose Destinations for typed navigation
- Kotlin Symbol Processing for generated code
- Kotlin Multiplatform in selected modules

The Android app also consumes `kalium` artifacts or substituted modules for shared business logic, networking, persistence-facing APIs, and other core capabilities.

## Relationship To The Wire Ecosystem

This repository is one part of Wire's broader client and platform ecosystem.

From the root README and build setup, the practical relationship is:

- this repo owns the Android application shell and Android-facing UI/features
- `kalium` provides shared logic and lower-level capabilities used by the Android app
- some repository content, such as `wireone-kmp`, extends beyond Android-only concerns and supports iOS or web-facing experiments

## Open-Source Constraints And Notes

The repository is open source, but the root README explicitly calls out important limits:

- some third-party service API keys are not included in the open-source project
- the open-source app differs from the Play Store binary in some integrated calling/audio-video signaling details
- trademark usage is restricted
- connecting custom-built apps to Wire servers is subject to Wire terms described in the root README

These constraints matter when documenting build behavior, flavor behavior, and dependency choices. They also explain why certain integrations are flavor-specific or absent from open-source builds.

## Where To Look

- Root overview: [`../README.md`](../README.md)
- Flavor and backend configuration: [`../default.json`](../default.json)
- Root Gradle build: [`../build.gradle.kts`](../build.gradle.kts)
- Root settings and module inclusion: [`../settings.gradle.kts`](../settings.gradle.kts)
- Included builds: [`../include_builds.gradle.kts`](../include_builds.gradle.kts)
