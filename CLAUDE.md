# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

### Essential Commands
- `./gradlew compileApp` - Compile the Wire Android client
- `./gradlew assembleApp` - Assemble the Wire Android client
- `./gradlew runApp` - Assemble and run the Wire Android client on a connected device
- `./gradlew runUnitTests` - Run all unit tests
- `./gradlew runAcceptanceTests` - Run all acceptance tests on a connected device
- `./gradlew testCoverage` - Generate test code coverage report
- `./gradlew staticCodeAnalysis` - Run static code analysis

### Running Tests for Specific Modules
- `./gradlew :app:testDevDebugUnitTest` - Run unit tests for app module (dev flavor, debug build)
- `./gradlew :features:cells:test` - Run tests for a specific feature module
- `./gradlew :core:ui-common:test` - Run tests for a core module

### Kalium Submodule Management
- `git submodule update --init --recursive` - Initialize and update the Kalium submodule (required after cloning)
- `git submodule update --remote --merge` - Update Kalium submodule to latest commit

### Important Setup Notes
- After cloning, you MUST run `git submodule update --init --recursive`
- Create `kalium/local.properties` with your Android SDK path (the IDE does not create this automatically)
- Format: `sdk.dir=/Users/YOUR_USER/Library/Android/sdk` (Mac) or equivalent for your OS
- Requires JDK 21, Android SDK, and Android NDK

## Project Architecture

### Module Structure

**app** - Main Android application module
- Entry point: `WireApplication`, `WireActivity`
- Contains UI screens, ViewModels, navigation, DI configuration, and utilities
- Multi-flavor support: prod, dev, staging, beta, internal, fdroid

**core/** - Shared libraries and infrastructure
- `ui-common` - Reusable Compose UI components (buttons, dialogs, lists, etc.)
- `navigation` - Navigation infrastructure using Compose Destinations
- `analytics` / `analytics-enabled` / `analytics-disabled` - Analytics modules (feature-flagged)

**features/** - Feature modules for specific functionality
- `cells` - File management and cloud storage UI
- `meetings` - Video/audio calling UI
- `sketch` - Drawing/canvas features
- `template` - Template for creating new feature modules

**kalium/** - Git submodule containing the Wire Kotlin Multiplatform SDK
- Provides business logic for authentication, messaging, encryption, calling, backups
- Key modules: `logic` (main SDK), `data`, `domain`, `core`, `test`, `tools`
- Wire Android depends on Kalium for all business logic via Dependency Injection

### Architecture Pattern: MVVM + Clean Architecture

**UI Layer** (`/app/src/main/kotlin/com/wire/android/ui/`)
- Jetpack Compose screens with Material 3 components
- ViewModels manage UI state using `StateFlow` or `mutableStateOf`
- State classes represent screen state (data classes)

**Business Logic Layer** (Kalium SDK)
- Access via `CoreLogic.getGlobalScope()` (session management) and `CoreLogic.getSessionScope(userId)` (user-specific operations)
- Use cases expose `Flow<T>` for reactive data or `suspend` functions for one-shot operations
- ViewModels collect from Kalium use case Flows and transform into UI state

**Data Flow Pattern**
```
Kalium Use Cases (Flow/suspend)
  → ViewModel (combines Flows, transforms to UI state)
    → Compose UI (collects StateFlow, renders state)
```

### Dependency Injection with Hilt

**DI Scopes**
- `@Singleton` / `SingletonComponent` - App-level dependencies (CoreLogic, WorkManager, NotificationManager)
- `@ViewModelScoped` / `ViewModelComponent` - ViewModel dependencies (use cases, mappers)
- `@ServiceScoped` / `ServiceComponent` - Service dependencies

**Key DI Modules** (`/app/src/main/kotlin/com/wire/android/di/`)
- `AppModule.kt` - App-level singletons
- `CoreLogicModule.kt` - Kalium SDK initialization
- `accountScoped/` - Account-specific modules providing Kalium use cases (CallsModule, ConversationModule, etc.)

**Important Qualifiers**
- `@KaliumCoreLogic` - Main Kalium SDK instance
- `@CurrentAccount` - Current logged-in user ID

### Navigation Architecture

Uses **Compose Destinations** library with KSP code generation:
- `@WireDestination` annotation marks Composable screens
- KSP generates type-safe navigation routes
- `Navigator` interface provides compile-safe navigation
- `MainNavHost.kt` configures the root navigation graph
- Multi-graph structure: login, newLogin, newConversation, home

**HomeDestination** options: Conversations, Settings, Vault, Archive, Support, WhatsNew, TeamManagement, Cells, Meetings

### Key Technologies

- **UI**: Jetpack Compose (Material 3 + Material 2 for compatibility)
- **DI**: Hilt (scoped to ViewModels, Activities, Services)
- **Async**: Kotlin Coroutines + Flow
- **Lifecycle**: ViewModel, LiveData
- **Image Loading**: Coil (with GIF/video support)
- **Persistence**: DataStore (preferences), Room (via Kalium)
- **Serialization**: kotlinx.serialization
- **Testing**: JUnit, Turbine (Flow testing), MockK, Kalium test utilities

## App Flavors

Wire Android has multiple flavors with different configurations (see `default.json`):

| Flavor   | Icon Color | Backend      | Logging | Dev Features | Purpose                                    |
|----------|------------|--------------|---------|--------------|---------------------------------------------|
| dev      | Red        | Staging      | Yes     | Yes          | Bleeding edge development, experimental     |
| staging  | Orange     | Staging      | Yes     | Yes          | QA testing with release-like app            |
| internal | Green      | Production   | Yes     | Yes          | Internal testing (may be deprecated)        |
| beta     | Blue       | Production   | Yes     | Yes          | Company dogfooding, pre-release features    |
| prod     | White      | Production   | No*     | No           | Public release on Google Play               |
| fdroid   | White      | Production   | No*     | No           | F-Droid build without closed-source libs    |

*Logs can be enabled by users in prod/fdroid but are NOT uploaded to third-party services.

## Code Organization Conventions

### UI Components (`/app/src/main/kotlin/com/wire/android/ui/`)
- Each screen has: `[FeatureName]Screen.kt` (Composable), `[FeatureName]ViewModel.kt`, `[FeatureName]State.kt`
- Use `@WireDestination` for screens that are navigation destinations
- Prefer stateless Composables with state hoisting

### ViewModels
- Use `@HiltViewModel` with constructor injection
- Manage state with `StateFlow` or Compose `mutableStateOf`
- Collect from Kalium use case Flows in `init` or via `viewModelScope.launch`
- Expose UI state as single immutable state object

### Use Cases
- Provided by Kalium's `GlobalScope` or `SessionScope`
- Access via DI in ViewModels
- Return `Flow<T>` for reactive data, `suspend fun` for one-shot operations
- Example: `observeConversationListUseCase()`, `sendMessageUseCase()`

### Testing
- Unit tests in `src/test/kotlin/`
- UI tests in `src/androidTest/kotlin/`
- Use Kalium test utilities from `kalium/test/mocks` and `kalium/test/data-mocks`
- Use Turbine for testing Flows: `flow.test { awaitItem() shouldBe expected }`

## Build System & Customization

### Convention Plugins (`/build-logic/`)
Reusable Gradle configuration:
- AndroidApplicationConventionPlugin
- AndroidLibraryConventionPlugin
- HiltConventionPlugin
- NavigationConventionPlugin (configures KSP for Compose Destinations)
- KoverConventionPlugin (test coverage)

### Custom Builds
Wire supports custom APKs for enterprise customers via external git repositories. Environment variables configure fetching custom resources and build flags. See `CUSTOMIZATION.md` for details.

### Build Configuration
- `default.json` - Default build flags and feature toggles for all flavors
- `custom-reloaded.json` (external) - Overrides for custom builds
- Custom resources can override any drawable, mipmap, or string resource

## Kalium SDK Integration

Kalium is a Kotlin Multiplatform SDK that handles all business logic. Wire Android is the presentation layer.

### Accessing Kalium
```kotlin
// Via DI in ViewModel
@HiltViewModel
class MyViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic
) : ViewModel() {

    // Get global scope (session management, auth)
    val globalScope = coreLogic.getGlobalScope()

    // Get session scope for a user (messaging, calls, etc.)
    val sessionScope = coreLogic.getSessionScope(userId)

    // Use cases from scopes
    val messages = sessionScope.messages.getConversationMessages(conversationId)
}
```

### Key Kalium Scopes
- **GlobalScope** - Session management, authentication, server configuration, migrations
- **SessionScope** - User-specific operations (conversations, messages, calls, connections, teams, etc.)

## Common Patterns

### State Management
```kotlin
data class MyScreenState(
    val isLoading: Boolean = false,
    val data: List<Item> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MyViewModel @Inject constructor(
    private val useCase: MyUseCase
) : ViewModel() {

    var state by mutableStateOf(MyScreenState())
        private set

    init {
        viewModelScope.launch {
            useCase.observeData().collect { data ->
                state = state.copy(data = data, isLoading = false)
            }
        }
    }
}
```

### Navigation
```kotlin
@WireDestination
@Composable
fun MyScreen(
    navigator: Navigator,
    viewModel: MyViewModel = hiltViewModel()
) {
    // Navigate to another screen
    navigator.navigate(NavigationCommand(OtherScreenDestination))

    // Navigate back
    navigator.navigateBack()
}
```

### Handling Feature Flags
Feature flags are configured in `default.json` and injected via `BuildConfigWrapper`:
```kotlin
@Inject
lateinit var buildConfigWrapper: BuildConfigWrapper

if (buildConfigWrapper.developerFeaturesEnabled) {
    // Show developer-only UI
}
```

## Contributing Notes

- Wire accepts bug fixes and code improvements via pull requests
- New features, UI/UX changes are decided by Wire team
- Must sign Contributor Agreement on first PR
- Run tests and linters before submitting PRs
- Add tests for all changes
- Use the PR template

## Important File Locations

- Main application: `/app/src/main/kotlin/com/wire/android/`
- UI screens: `/app/src/main/kotlin/com/wire/android/ui/`
- DI configuration: `/app/src/main/kotlin/com/wire/android/di/`
- Navigation: `/app/src/main/kotlin/com/wire/android/navigation/`
- Kalium submodule: `/kalium/`
- Build configuration: `/buildSrc/`, `/build-logic/`
- Flavor configuration: `/default.json`
