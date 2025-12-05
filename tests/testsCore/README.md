# Android UI Tests – Local Execution & Allure Reporting Guide

This document explains how to run Android UI tests locally, how to filter which tests execute, and how to generate Allure HTML reports.

Important:
- This guide is for local execution only.
- CI pipelines may follow different steps or use additional automation.
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
