# Android UI Tests – Local Execution & Allure Reporting Guide

This document explains how to run Android UI tests locally, how to filter which tests execute, and how to generate Allure HTML reports.

Important:
- This guide is for local execution only.
- CI pipelines may follow different steps or use additional automation.

---

## Supported Test Annotations

UI tests support custom annotations to organize and filter test runs.

### TestCaseId

@TestCaseId("TC-8602")

Filtering is implemented through:

- TestCaseIdFilterRule
- CategoryFilterRule
- TagFilterRule 

Tests that do not match the filter are marked as SKIPPED, allowing Allure to display accurate results.

---

# 1. Run Tests by TestCaseId

Execute a specific test case mapped with @TestCaseId.

Command:

./gradlew :tests:testsCore:connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.testCaseId=TC-8602

Example annotation:

@TestCaseId("TC-8602")

---

# 2. Run Tests by Category

Categories allow grouping tests such as:

- criticalFlow
- regression
- smoke

Example annotation:

@Category("criticalFlow")

Run tests by category:

./gradlew :tests:testsCore:connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.category=criticalFlow

---

# 3. Run Tests by Tag

Tags allow key/value-based filtering.

Example annotation:

@Tag(key = "feature", value = "calling")

Run matching tag:

./gradlew :tests:testsCore:connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.tagKey=feature \
-Pandroid.testInstrumentationRunnerArguments.tagValue=calling

Both tagKey and tagValue must be provided.

---

# Pulling Allure Results From Device

Allure results are stored on the emulator/device at:

/sdcard/googletest/test_outputfiles/allure-results

Pull the results:

adb exec-out sh -c 'cd /sdcard/googletest/test_outputfiles && tar cf - allure-results' > allure-results.tar

Extract locally:

rm -rf allure-results
mkdir allure-results
tar xf allure-results.tar -C allure-results

---

# Open Allure Report (Local)

Serve the report locally:

allure serve allure-results/allure-results

This opens an interactive HTML report showing:

- Passed tests
- Failed tests
- Skipped tests
- Attachments and logs (when implemented)
