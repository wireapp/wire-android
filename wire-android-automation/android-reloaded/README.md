# Setup
## Simulator Automation Setup
        
1. Download either Android Studio or "Command line tools only" from [Android Developers Website](https://developer.android.com/studio)
        
2. On **Android Studio**: Click on Configure > SDK Manager > SDK tools and download the following tools:
- Android SDK Build-Tools
- Android SDK Command-line Tools
- Android SDK Platform Tools
3. On **Command line tools**: Unpack into local directory and run:
- `sdkmanager --licenses` (to accept all licenses)
- `sdkmanager --install "build-tools;29.0.3"` (needed since appium 1.20.x)
- `sdkmanager --install platform-tools`
4. Check [GridDeployment.groovy](../android/GridDeployment.groovy) for currently used versions (APPIUMVERSION, UIAUTOMATOR2VERSION)
5. Install **Appium 2** with the version above: `npm install appium@<version>` (Please don't  use `-g` option)
6. Set ANDROID_HOME and JAVA_HOME in your environment (Usually in `~/.bash_profile`)
7. Install uiautomator2 driver: `npm install appium-uiautomator2-driver@<version>` (Please don't  use `-g` option or `appium driver install`)
8. Run appium with: `ANDROID_HOME=<path> npx appium --relaxed-security`

# Setup Emulator with Android Studio
* To set up your own emulator, you can use Android Studio
* Make sure, that you have "Android Emulator" in SDK Tools in Android Studio installed
* After this is done, on Android Studio Start Window, go to Configure > AVD Manager
* Select "Create virtual device" and select a device of your choice
* By doing so, you also have the option to download various Android images, which you can use on your emulator

# Finding locators
There are two possibilities for finding locators with [Appium-Inspector](https://github.com/appium/appium-inspector/releases):
-   By running a testcase with a break point, and attaching the appium server to this session
-   by starting an appium session directly through appium inspector

The benefit of using a break point is that it can take care of setting up the test, so that you don't need to manually create the circumstances in which the locator will show. It can however, be a bit tricky to connect to the session sometimes.

### By running a test

* Run a testcase which has a breakpoint in debug mode. **Alternative possibility:**
You can add a step `And I wait for 200 seconds` 
to make the test execution pause for X seconds, which can be quicker than 
starting a debug run. Just make sure to remove this before commiting. 
* Open the active Appium Inspector
* Press **Command + n**
* Click **Attach to session...** 
* Click the refresh button on the drop-down bar below until the Session ID shows up
* Click Attach to Session

### Without running a test

* Open Appium Inspector and start a server
* Press **Command + n** or press the magnifier icon
* Underneath "Desired Capabilities", add the following values:
```
{
  "platformName": "Android",
  "platformVersion": "9",
  "deviceName": "emulator-5554",
  "app": "/Users/USERNAME/Downloads/wire-internal-release-3.60.2182.apk"
}
```  
* Click on "Save as..." and save the values so that you do not have to re-enter this the next time
* Click on "Start Session"

# Troubleshooting
* Enable Developer mode on the device under test and make sure it is visible in `adb device -l` output when connected
* Run Testing Gallery once it is installed and press the "FIX" button on every check
* Make sure you have Android platform tools only installed once. Before you start the setup, you can uninstall everything with:
```
brew cask remove android-platform-tools
brew cask reinstall android-sdk
```
* If you have used adb with sudo before, make sure both files `~/.android/adbkey` and `~/.android/adbkey.pub` are writable by the current user
* Kill all appium server processes before running the first test to avoid configuration issues
* *It is impossible to create a new session because 'createSession' which takes HttpClient, InputStream and long was not found or it is not accessible* or *Original error: Error getting device API level* could happen because the connection is not accepted on the devices. Run `adb devices -l` and if you see *unauthorized usb* go to the phone in the lab and press "Allow" on the dialog.
* If automation was run on device on another computer, you might need to uninstall 2 applications before new automation is about to start:
`io.appium.uiautomator2.sertver` and `io.appium.uiautomator2.sertver.test`
* If automation is unable to start - read stack traces. If it doesnt help - kill appium witn `killall node`, run appium locally with 
`appium --relaxed-security` and read actual statements, which are executed by Appium 


# Test execution
* Create new Run configuration (Run -> Edit Configurations -> Add new -> Maven)
* Set Working directory to the zautomation root, for example `/Users/elf/code/zautomation/tests/`
* Set Command line to `--also-make --projects android clean install`
* Go to **Runner**
* Uncheck **Use project settings**
* Uncheck **Skip tests**
* Add new property `appPath` and enter path to .apk
* Add new property `package` and enter `com.wire.internal` or the package depending on the .apk
* Add new property `picklejar.tags`
* Set the property to `@torun` or the id of the test you want to run
* Set environment variables for [Credentials](../../README.md#credentials) and [Federation](../../README.md#federation)

## Debug
* Create another Run configuration (Run -> Edit Configurations -> Add new -> Android debug)
* Set Working directory to the zautomation root, for example `/Users/elf/code/zautomation/tests/`
* Set Command line to `--also-make --projects android -Dmaven.surefire.debug clean install`

## Test Execution via Command Line
* Go to the test directory of zautomation und use the following command to execute tests from command line:
`mvn -DandroidToolsPath=/Users/elf/zautomation/tests/tools/android/ -DappPath=/Users/elf/wire-internal-release-xyz.apk --also-make --projects android clean install -Dpicklejar.tag=@torun`

* Additional Parameters that could be used:
`-P isOnGrid | -Durl | -Dpicklejar.parallelism | -DbrowserName | -Dmaven.surefire.debug | -Dsurefire.rerunFailingTestsCount | -Dcom.wire.calling.env | -Dpackage`

# Differences between production build
We use special builds for testing instead of the production app. This is a (incomplete) list of differences between the production build and the dev/experimental/candidate builds:
* **Backend Selection Dialog** When the app starts you see a dialog to choose between `prod`, `qa-demo` or `staging`. Selecting qa-demo or staging will point the app always to qa-demo or staging backends. The `prod` choice works a bit different: It uses the backend endpoint from the `default.json`. The `default.json` depends on the build job and can contain different backends (especially for custom customer builds).

# Special Testcases
## Upgrade
### Locally

To run upgrade tests locally, you need to have 2 different apks: 
* actual build you want to test (e.g. `com.wire.android-v4.10.0-59599-beta-release.apk`)
* old build which you want to upgrade from (e.g. `com.wire.android-v4.9.2-49229-beta-release.apk`)

To execute the tests you need to provide one additional parameters in the configurations:
* `-DoldAppPath=/Users/elf/com.wire.android-v4.9.2-49229-beta-release.apk`

### Automation Grid
If you want to perform upgrade tests in our automation grid, it will automatically pick the previous build and the latest build for each track.
If you want to test specific build, you need to provide the new build number `AppBuildNumber` (e.g. `2418`) and as well the old build number `OldBuildNumber` (e.g.2417`) in the parameters in the Jenkins Job.
