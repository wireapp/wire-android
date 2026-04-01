# Features

This document groups the repository's visible capabilities into feature areas and notes where each capability is implemented in the Android repo.

The descriptions in this document are derived from module names, package structure, manifests, flavor configuration, and app/runtime wiring. For architectural context, see [Architecture](./architecture.md).

## Purpose

The repository combines end-user features, platform integration features, and internal tooling features. Not every capability is isolated into its own Gradle module; many live directly in the app module and are supported by `core/*`, `features/*`, and `kalium`.

## Authentication And Account Access

### Purpose

The app supports sign-in, account entry flows, and related account-state transitions.

### Key Components

- app packages under `ui/authentication`, `ui/newauthentication`, and `ui/registration`
- login support packages under `feature/login`
- deep-link and SSO-related manifest entries
- session and account use cases provided through Hilt from `kalium`

### How It Connects

Authentication UI lives primarily in the app module, while account/session logic is adapted from `kalium`.

### Where To Look

- [`../app/src/main/kotlin/com/wire/android/ui/authentication`](../app/src/main/kotlin/com/wire/android/ui/authentication)
- [`../app/src/main/kotlin/com/wire/android/ui/newauthentication`](../app/src/main/kotlin/com/wire/android/ui/newauthentication)
- [`../app/src/main/kotlin/com/wire/android/ui/registration`](../app/src/main/kotlin/com/wire/android/ui/registration)
- [`../app/src/main/kotlin/com/wire/android/feature/login`](../app/src/main/kotlin/com/wire/android/feature/login)

## Conversations, Messaging, And Home UI

### Purpose

The app includes conversation-centric flows for browsing conversations, opening message threads, composing messages, sharing content, and navigating the main logged-in experience.

### Key Components

- `ui/home/conversations`
- `ui/home/conversationslist`
- `ui/home/messagecomposer`
- `ui/sharing`
- `ui/joinConversation`
- `ui/home/newconversation`

### How It Connects

The app module owns much of the conversation UI shell, while message and conversation logic comes from `kalium` via injected use cases and session scope access.

### Where To Look

- [`../app/src/main/kotlin/com/wire/android/ui/home`](../app/src/main/kotlin/com/wire/android/ui/home)
- [`../app/src/main/kotlin/com/wire/android/ui/sharing`](../app/src/main/kotlin/com/wire/android/ui/sharing)
- [`../app/src/main/kotlin/com/wire/android/navigation`](../app/src/main/kotlin/com/wire/android/navigation)

## Calling And Meetings

### Purpose

The repository contains both calling flows and meetings-related UI surfaces.

### Key Components

- calling activities in the app manifest
- `ui/calling` packages in the app module
- `features:meetings`
- call-related analytics and lifecycle observation in `WireApplication`

### How It Connects

Calling uses dedicated Android activities and app-level coordination, while meetings UI is split into a dedicated feature module. Lower-level calling logic is provided through shared dependencies rather than fully implemented in the UI layer.

### Where To Look

- [`../app/src/main/kotlin/com/wire/android/ui/calling`](../app/src/main/kotlin/com/wire/android/ui/calling)
- [`../features/meetings/src/main/java/com/wire/android/feature/meetings`](../features/meetings/src/main/java/com/wire/android/feature/meetings)
- [ADR 0002: Calling Activities Refactor](./adr/0002-calling-activities-refactor.md)

## Cells And File Management

### Purpose

The repository contains a dedicated Cells feature area for file and storage-oriented interactions.

### Key Components

- `features:cells`
- Cells UI for create, rename, recycle bin, search, public links, versioning, tags, and downloads
- app packages such as `ui/home/cell` and `ui/home/vault`

### How It Connects

The feature module provides a distinct UI/domain surface, while backing logic and data capabilities rely on `kalium` and app-level DI wiring.

### Where To Look

- [`../features/cells/src/main/java/com/wire/android/feature/cells`](../features/cells/src/main/java/com/wire/android/feature/cells)
- [`../app/src/main/kotlin/com/wire/android/ui/home/cell`](../app/src/main/kotlin/com/wire/android/ui/home/cell)
- [`../app/src/main/kotlin/com/wire/android/ui/home/vault`](../app/src/main/kotlin/com/wire/android/ui/home/vault)

## Sync, Background Processing, And Notifications

### Purpose

The app includes synchronization flows, background workers, and notification handling.

### Key Components

- `features:sync`
- app sync UI packages
- WorkManager workers
- notification packages and notification support modules
- lifecycle-driven observers in `WireApplication`

### How It Connects

This feature area mixes UI, background work, and platform services. The Android repo owns worker execution and notification presentation, while underlying state often comes from `kalium`.

### Where To Look

- [`../features/sync/src/main`](../features/sync/src/main)
- [`../app/src/main/kotlin/com/wire/android/ui/home/sync`](../app/src/main/kotlin/com/wire/android/ui/home/sync)
- [`../app/src/main/kotlin/com/wire/android/workmanager`](../app/src/main/kotlin/com/wire/android/workmanager)
- [`../core/notification`](../core/notification)

## Security, Admin, And Compliance Surfaces

### Purpose

The app contains several security-oriented and enterprise-oriented capabilities.

### Key Components

- E2EI enrollment and certificate-related UI
- legal hold UI
- app lock flows
- biometric support
- EMM and managed configurations support
- screenshot-censoring and other security-related use cases exposed through DI

### How It Connects

Some of these capabilities are user-facing screens, while others are policy or compliance surfaces integrated into lifecycle, configuration, and session flows.

### Where To Look

- [`../app/src/main/kotlin/com/wire/android/ui/e2eiEnrollment`](../app/src/main/kotlin/com/wire/android/ui/e2eiEnrollment)
- [`../app/src/main/kotlin/com/wire/android/ui/legalhold`](../app/src/main/kotlin/com/wire/android/ui/legalhold)
- [`../app/src/main/kotlin/com/wire/android/ui/home/appLock`](../app/src/main/kotlin/com/wire/android/ui/home/appLock)
- [`../app/src/main/kotlin/com/wire/android/emm`](../app/src/main/kotlin/com/wire/android/emm)
- [ADR 0008: Introducing EMM Config Capabilities](./adr/0008-introducing-emm-config-capabilities.md)

## Settings, Profile, And Device Management

### Purpose

The app includes account settings, device management, profile management, and related support screens.

### Key Components

- `ui/settings`
- `ui/userprofile`
- `ui/settings/devices`
- QR/profile-related packages
- self-device and profile destinations referenced in navigation

### How It Connects

These flows live primarily in the app module and consume injected session-aware use cases for account and device operations.

### Where To Look

- [`../app/src/main/kotlin/com/wire/android/ui/settings`](../app/src/main/kotlin/com/wire/android/ui/settings)
- [`../app/src/main/kotlin/com/wire/android/ui/userprofile`](../app/src/main/kotlin/com/wire/android/ui/userprofile)

## Developer And Internal Tooling Features

### Purpose

The repository includes internal-only or debugging-oriented capabilities that are exposed selectively by flavor.

### Key Components

- debug screens
- feature-flag visibility controls
- logging configuration differences
- flavor-specific source sets for public/private and FOSS/non-free behavior

### How It Connects

These capabilities are part of the shipped architecture even when they are not enabled in every flavor. They are selected through build configuration and runtime flags rather than existing as a completely separate application.

### Where To Look

- [`../app/src/main/kotlin/com/wire/android/ui/debug`](../app/src/main/kotlin/com/wire/android/ui/debug)
- [`../app/src/main/kotlin/com/wire/android/util/debug`](../app/src/main/kotlin/com/wire/android/util/debug)
- [`../default.json`](../default.json)

## Cross-Platform And Shared UI Experiments

### Purpose

The repository also contains limited cross-platform UI work beyond the main Android app.

### Key Components

- `core:ui-common-kmp`
- `wireone-kmp`

### How It Connects

These modules are not the main app path, but they show that parts of the UI stack are being shared across Android, iOS, and web-facing targets.

### Where To Look

- [`../core/ui-common-kmp`](../core/ui-common-kmp)
- [`../wireone-kmp`](../wireone-kmp)
- [`../wireone-kmp/README.md`](../wireone-kmp/README.md)
