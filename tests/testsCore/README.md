# Android UI Tests – Local Execution & Allure Reporting Guide

This document explains how to run Android UI tests locally, how to filter which tests execute, and how to generate Allure HTML reports.

Important:
- This guide is for local execution only.
- CI pipelines may follow different steps or use additional automation.

---

## Supported Test Annotations

UI tests support custom annotations to organize and filter test runs.

TestCaseId:
@TestCaseId("TC-8602")

Category:
@Category("criticalFlow")

Tag:
@Tag(key = "feature", value = "calling")

---

## Filtering System (Latest Implementation)

Filtering is now handled by a single JUnit filter:

TaggedFilter  
(com.wire.android.tests.support.suite.TaggedFilter)

This filter reads instrumentation arguments:
- testCaseId
- category
- tagKey
- tagValue

Only tests that MATCH the provided filters are executed.  
All other tests are fully excluded and do NOT appear in Allure reports.
---

# 1) Run Tests by TestCaseId

Execute only tests annotated with a specific @TestCaseId.

Command:
./gradlew :tests:testsCore:connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.filter=com.wire.android.tests.support.suite.TaggedFilter \
-Pandroid.testInstrumentationRunnerArguments.testCaseId=TC-8602

Example annotation:
@TestCaseId("TC-8602")

---

# 2) Run Tests by Category

Categories allow grouping tests by purpose (criticalFlow, regression, smoke, etc).

Example annotation:
@Category("criticalFlow")

Run tests in a category:
./gradlew :tests:testsCore:connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.filter=com.wire.android.tests.support.suite.TaggedFilter \
-Pandroid.testInstrumentationRunnerArguments.category=criticalFlow

---

# 3) Run Tests by Tag

Tags allow key/value-based filtering.

Example annotation:
@Tag(key = "feature", value = "calling")

Run tests matching a tag:
./gradlew :tests:testsCore:connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.filter=com.wire.android.tests.support.suite.TaggedFilter \
-Pandroid.testInstrumentationRunnerArguments.tagKey=feature \
-Pandroid.testInstrumentationRunnerArguments.tagValue=calling

Note: Both tagKey and tagValue must be provided.

---

# Pulling Allure Results From Device

Allure results are stored on the device at:
/sdcard/googletest/test_outputfiles/allure-results

Pull results:
adb exec-out sh -c 'cd /sdcard/googletest/test_outputfiles && tar cf - allure-results' > allure-results.tar

Extract locally:
rm -rf allure-results
mkdir allure-results
tar xf allure-results.tar -C allure-results

---

# Open Allure Report (Local)

Serve the report:
allure serve allure-results/allure-results

This opens an interactive HTML report showing:
- Passed tests
- Failed tests
- Executed test steps
- Attached screenshots for failures
- Only the tests that actually ran (noise-free reporting)
