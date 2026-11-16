# Android UI Tests – Local Execution & Allure Reporting Guide

This document explains how to run Android UI tests locally, how to filter which tests execute, and how to generate Allure HTML reports.

Important:
- This guide is for local execution only.
- CI pipelines may follow different steps or use additional automation.
---
## TC-12255 and TC-12260 — Interruptible downloads

These cases must interrupt a genuinely active download. Use a controlled slow
connection; file size alone does not guarantee that the download remains active.
For a real Android device that supports download rate limiting, first record:

```bash
adb -s DEVICE_SERIAL shell settings get global ingress_rate_limit_bytes_per_second
```

A temporary value of `1000000` requests approximately 8 Mbit/s:

```bash
adb -s DEVICE_SERIAL shell settings put global ingress_rate_limit_bytes_per_second 1000000
```

Restore the recorded value after the case, including failures. If the original
value was `null`, delete the setting instead:

```bash
adb -s DEVICE_SERIAL shell settings delete global ingress_rate_limit_bytes_per_second
```

A successful settings write does not prove that a device enforces the limit.
The tests still require active loading before cancellation/disconnection.
Older devices without an effective limiter need a controlled connection.
This validation setup does not change the normal CI workflow's bandwidth.

---
## TC-12261 — Test-service access while the device is offline

This case keeps Wi-Fi and mobile data disabled while TeamOwner replaces the
online PDF through test service. The offline replacement request uses an ADB
reverse connection. Configure its loopback URL with the instrumentation argument
`offlineTestServiceUrl`; it defaults to the compiled test-service URL for existing
local commands. The test checks that this URL uses `127.0.0.1`.

For the service on node018 (`192.168.2.18:8082`), keep this SSH tunnel running in
a separate terminal on the machine connected to the Android device. Replace
`YOUR_SSH_USERNAME` with your username on node018.

```bash
ssh -NT -o ExitOnForwardFailure=yes \
  -L 127.0.0.1:18082:192.168.2.18:8082 \
  YOUR_SSH_USERNAME@192.168.2.18
```

In the repository terminal, select the target device using `ANDROID_SERIAL`
when more than one device is connected. Forward its loopback port and run:

```bash
adb reverse tcp:18082 tcp:18082
TEST_SERVICE_URL=http://192.168.2.18:8082 \
  ./gradlew :tests:testsCore:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.testCaseId=TC-12261 \
  -Pandroid.testInstrumentationRunnerArguments.offlineTestServiceUrl=http://127.0.0.1:18082
```

After the run, remove only this forwarding rule and stop the SSH tunnel with
Ctrl+C in its terminal:

```bash
adb reverse --remove tcp:18082
```

CI configures this connection in `run_ui_tests.sh` when TC-12261 is selected,
including through a regression/category run or an explicit retry list. A Python
relay on the runner forwards the replacement request to the resolved test-service
URL. Each device attempt creates its reverse rule with `--no-rebind` and removes
that rule and stops its relay on exit, including test failures. Other service
requests use their original URL.

---
## Supported Test Annotations
Tests use custom annotations that enable filtering and reporting:

TestCaseId:  
@TestCaseId("TC-8602")

Category:  
@Category("criticalFlow")

Tag:  
@Tag(key = "feature", value = "calling")
---
## Filtering System (Latest Implementation)

Filtering is handled by one JUnit filter:

**com.wire.android.tests.support.suite.TaggedFilter**

It reads these arguments:
- testCaseId
- category
- tagKey
- tagValue

Only tests matching the filter **run**.  
All other tests are fully excluded and do NOT appear in Allure reports.

---
# 1) Run Tests by TestCaseId

Example: run only tests annotated with @TestCaseId("TC-8602")

## Command:

./gradlew :tests:testsCore:connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.testCaseId=TC-8602
---

# 2) Run Tests by Category

Example annotation:

@Category("criticalFlow")

## Command:

./gradlew :tests:testsCore:connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.category=criticalFlow

---
# 3) Run Tests by Tag

Example annotation:

@Tag(key = "feature", value = "calling")

## Command:
./gradlew :tests:testsCore:connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.tagKey=feature \
-Pandroid.testInstrumentationRunnerArguments.tagValue=calling

Note: Only runs tests where both key AND value match.

Important:
- CI passes `android.testInstrumentationRunnerArguments.appPackage` explicitly.
- Local runs keep using the package selected in each test setup.
- Change the local `UiAutomatorSetup.APP_*` selection in the test when you want a different local flavor.
- Example values:
  - `com.wire.internal` for alpha release candidate
  - `com.wire.android.internal` for alpha beta
  - `com.wire` for production
---

## Pulling Allure Results From Device

Install allure

## Allure results are stored on the device at:  
/sdcard/googletest/test_outputfiles/allure-results

## To pull results from device:  
adb exec-out sh -c 'cd /sdcard/googletest/test_outputfiles && tar cf - allure-results' > allure-results.tar

## To extract locally into the project folder:  
rm -rf allure-results  
mkdir allure-results  
tar xf allure-results.tar -C allure-results

---
# Open Allure Report (Local)

## Run:
allure serve allure-results/allure-results

## This opens an interactive HTML report showing:
- Passed tests
- Failed tests with screenshot
- Tags (TestCaseId, Category, feature:value)
- Only executed tests (no noise)

## Testiny reporting

`@TestCaseId` values refer to **Wire-Prism (project 17)**. The original Android
IDs are retained in Testiny's `OLD_ID` field; local filters use the new Prism IDs.

Local reporting is off unless configured. To enable it, supply
`testinyProjectName=Wire-Prism`, `testinyRunName` and `testinyApiKey` as
instrumentation arguments. Use a key authorized for Wire-Prism and a separate
validation run when checking the integration. Keep credentials out of committed
files and terminal output.
