# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Wire for Android is a secure messaging and collaboration client built with Jetpack Compose and Kotlin. The app supports end-to-end encrypted messaging, voice/video calling, and is built on top of the Kalium SDK (included as a git submodule at `kalium/`).

**Requirements:** JDK 21, Android SDK, Android NDK

## Build Commands

```bash
# Compile the app
./gradlew compileApp

# Assemble the app
./gradlew assembleApp

# Run the app on a connected device
./gradlew runApp

# Run all unit tests
./gradlew runUnitTests

# Run acceptance tests (requires connected device)
./gradlew runAcceptanceTests

# Generate test coverage report
./gradlew testCoverage

# Static code analysis
./gradlew staticCodeAnalysis

# Run a specific test class
./gradlew :app:testDevDebugUnitTest --tests "com.wire.android.feature.SomeUseCaseTest"

# Run tests for a specific module
./gradlew :features:cells:testDebugUnitTest
```

## Setup and Submodule Management

After cloning, initialize the Kalium submodule:

```bash
git submodule update --init --recursive
```

When syncing after pull or switching branches:

```bash
git submodule update --remote --merge
```

Both `local.properties` and `kalium/local.properties` must point to your Android SDK path. The IDE creates the root `local.properties` automatically, but you must manually create `kalium/local.properties`.

## Architecture

### Module Structure

The project uses a modular architecture with modules organized by functional area:

**`app/`** - Main application module containing:
- `di/` - Hilt dependency injection setup with `@HiltAndroidApp`
- `ui/` - Jetpack Compose UI screens organized by feature (home, settings, calling, registration, etc.)
- `navigation/` - Navigation graph using Jetpack Navigation Compose
- `feature/` - App-level use cases and business logic
- `mapper/` - Transforms between Kalium domain models and UI models
- `notification/` - Notification handling for calls and messages
- `datastore/` - Encrypted DataStore for user preferences

**`core/`** - Core utility modules:
- `analytics/` - Analytics abstraction layer
- `analytics-enabled/` - Countly analytics implementation
- `analytics-disabled/` - No-op analytics (for F-Droid)
- `navigation/` - Navigation annotations and utilities
- `ui-common/` - Shared Compose UI components and theme

**`features/`** - Feature modules (experimental):
- `cells/` - Cell-based storage UI
- `meetings/` - Meeting integration features
- `sketch/` - Drawing/sketching features

**`tests/`** - Test support modules:
- `testsCore/` - Core test utilities
- `testsSupport/` - Test fixtures and helpers

**`kalium/`** - Git submodule containing the Kalium SDK (see `kalium/CLAUDE.md` for SDK-specific details)

**`buildSrc/`** - Build configuration:
- `customization/` - Custom build flavor system (see CUSTOMIZATION.md)
- `flavor/` - Product flavor definitions

**`build-logic/`** - Gradle convention plugins for consistent build configuration across modules

### Dependency Injection

The app uses **Hilt** for dependency injection:
- Application: `@HiltAndroidApp` on `WireApplication`
- ViewModels: `@HiltViewModel` with constructor injection
- Modules: Account-scoped modules in `di/accountScoped/` provide per-user dependencies
- CoreLogic: Kalium's `CoreLogic` is injected via `@KaliumCoreLogic` qualifier

### UI Architecture

**Jetpack Compose** with MVVM pattern:
- Screens are `@Composable` functions (e.g., `ConversationScreen`)
- State is managed via `@HiltViewModel` classes exposing `State` data classes
- Navigation uses Jetpack Navigation Compose with type-safe arguments
- UI follows Material Design 3 with custom Wire theme in `core:ui-common`

**Key UI Patterns:**
- Screen composables receive a `Navigator` for navigation actions
- ViewModels use `StateFlow` for state and `Channel` for one-time events
- Mappers (in `app/mapper/`) transform domain models to UI models

### App Flavors

Six flavors defined in `default.json` (see README.md for details):
- **dev** (🔴 red) - Development with staging backend, all features enabled
- **staging** (🟡 orange) - QA testing with staging backend
- **internal** (🟢 green) - Currently unused
- **beta** (🔵 blue) - Internal dogfooding with production backend
- **prod** (⚪ white) - Production release
- **fdroid** (⚪ white) - F-Droid build without closed-source dependencies

Flavor-specific behavior is controlled via `default.json` and can be customized (see CUSTOMIZATION.md).

### Build Customization

The build system supports custom flavors via environment variables or `local.properties`:
- `CUSTOM_REPOSITORY` - Git repo with custom build configs
- `CUSTOM_FOLDER` / `CLIENT_FOLDER` - Path to custom build directory
- Custom `custom-reloaded.json` overrides `default.json` values
- Custom resources overwrite standard resources for branding

See CUSTOMIZATION.md for complete documentation.

## Testing

**Test Types:**
- Unit tests: `app/src/test/` and module `src/test/`
- Instrumented tests: `app/src/androidTest/` using Compose testing and Hilt
- Acceptance tests: UIAutomator-based end-to-end tests

**Test Frameworks:**
- JUnit 5 for test structure
- MockK for mocking (avoid `mockk-android`, use `mockk` with Robolectric)
- Compose Testing for UI tests
- Hilt testing with `@HiltAndroidTest`
- Robolectric for Android unit tests without emulator

**Running Specific Tests:**
```bash
# Single test class
./gradlew :app:testDevDebugUnitTest --tests "com.wire.android.feature.MyUseCaseTest"

# Single test method
./gradlew :app:testDevDebugUnitTest --tests "com.wire.android.feature.MyUseCaseTest.should validate input"
```

## Code Conventions

- **Language:** Kotlin with coroutines for async operations
- **UI:** Jetpack Compose (no XML layouts)
- **State:** `StateFlow` for state, `Flow` for reactive streams
- **Error Handling:** Kalium uses `Either<Failure, Success>` from `core:util`
- **Date/Time:** `kotlinx-datetime` types (`Instant`, `LocalDateTime`)
- **Serialization:** `kotlinx.serialization` with `@Serializable`
- **Null Safety:** Non-null by default, use `?` only when necessary
- **Naming:** Compose functions are PascalCase (e.g., `ConversationScreen`)

## Kalium SDK Integration

The Kalium submodule provides core messaging functionality. Key integration points:

**CoreLogic Access:**
```kotlin
@Inject
@KaliumCoreLogic
lateinit var coreLogic: Lazy<CoreLogic>

// Access session-scoped logic
val userSession = coreLogic.getSessionScope(userId)
userSession.messages.sendTextMessage(conversationId, text)
```

**Use Cases:**
Most business logic is in Kalium use cases. The app layer adds UI-specific use cases in `app/feature/` and `di/accountScoped/` modules.

**Database:**
Kalium manages two SQLDelight databases (see `kalium/CLAUDE.md`):
- `UserDatabase` - Per-user data
- `GlobalDatabase` - Shared configuration

## Common Patterns

**ViewModels:**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val someUseCase: SomeUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val state: StateFlow<MyState> = /* state management */
}
```

**Navigation:**
```kotlin
@Composable
fun MyScreen(navigator: Navigator) {
    // Use navigator for navigation actions
    Button(onClick = { navigator.navigate(NavigationItem.OtherScreen) })
}
```

**Mappers:**
```kotlin
// Transform Kalium domain models to UI models
fun Message.toUIMessage(): UIMessage = UIMessage(
    id = id.value,
    text = content.text,
    // ...
)
```

## Important Notes

- Main branch: `develop` (not `main`)
- Contributions: Bug fixes and improvements only (see CONTRIBUTING.md)
- Translations: Via Crowdin (see CONTRIBUTING.md)
- Logging: Controlled per-flavor; never log sensitive content
- Security: E2E encryption handled by Kalium; validate at app boundaries
