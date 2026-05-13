# WireOne KMP Module

This module contains shared WireOne UI for iOS and Web (Wasm).

## Web (Compose Wasm)

### Run from terminal

```bash
./gradlew :wireone-kmp:wireoneWebRun
```

Open the URL printed by webpack-dev-server (for example `http://localhost:8083/`).

### Build browser artifacts

```bash
./gradlew :wireone-kmp:wireoneWebBuild
```

## Run from Android Studio

### Option 1: Gradle tool window
1. Open Gradle tool window.
2. Go to `wireone-kmp > Tasks > other`.
3. Run `wasmJsBrowserDevelopmentRun`.

### Option 2: Run configuration (recommended)
1. Open `Run > Edit Configurations...`.
2. Add new `Gradle` configuration.
3. Name: `WireOne Web`.
4. Gradle project: `wire-android (root)`.
5. Tasks: `:wireone-kmp:wasmJsBrowserDevelopmentRun`.
6. Run config and open URL from logs.

## iOS host app

Deployment target: **iOS 15+**

### Run
1. Open `wireone-kmp/iosApp/WireOne/WireOne.xcodeproj` in Xcode.
2. In target `WireOne` set `Signing & Capabilities > Team` (one-time per developer).
3. Select an iOS Simulator.
4. Run the app (`Cmd + R`).

### Build integration
- The app includes a Run Script phase that builds the KMP framework:
  - `bash "${PROJECT_DIR}/../scripts/build-kmp-framework.sh"`
- The app links and embeds:
  - `wireone-kmp/build/bin/iosSimulatorArm64/debugFramework/WireOneKmp.framework`
- The app links SQLCipher through a local SwiftPM linkage package:
  - `wireone-kmp/iosApp/WireOne/_internal_linkage_SwiftPMImport/Package.swift`
  - The package depends on `https://github.com/skiptools/swift-sqlcipher.git` from `1.4.0`.
- The KMP framework build passes `-Dkalium.apple.linkSqlite=false` so Kalium/SQLDelight does not also link Apple's system `sqlite3`.

### Notes
- For device builds, the script switches to `:wireone-kmp:linkDebugFrameworkIosArm64`.
- WireOne initializes Kalium with persistent iOS storage and `KaliumConfigs()` encryption enabled, so the sample exercises the SQLCipher-backed database path.

---
