# WireOne iOS host app

Deployment target: **iOS 15+**

## Run
1. Open `wireone-kmp/iosApp/WireOne/WireOne.xcodeproj` in Xcode.
2. In target `WireOne` set `Signing & Capabilities > Team` (one-time per developer).
3. Select an iOS Simulator.
4. Run the app (`Cmd + R`).

## Build integration
- The app includes a Run Script phase that builds the KMP framework:
  - `bash "${PROJECT_DIR}/../scripts/build-kmp-framework.sh"`
- The app links and embeds:
  - `wireone-kmp/build/bin/iosSimulatorArm64/debugFramework/WireOneKmp.framework`

## Notes
- For device builds, the script switches to `:wireone-kmp:linkDebugFrameworkIosArm64`.
