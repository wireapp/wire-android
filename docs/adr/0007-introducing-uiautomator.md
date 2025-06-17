# 7. Introducing UIAutomator for integrated testing

Date: 2025-05-06

## Status

Accepted

## Context

QA wants to migrate from Appium to a new framework to run tests and being closed to the source code that we are testing.
The tests nowadays are running on an emulator with an apk. After meeting the QA we evaluated options, Espresso and UIAutomator.

## Decision

UIAutomator seems to be the best option for us, as it is a framework that is already in the Android SDK and it is more flexible than Espresso, this last one is more limited to the app under test because of mocks and stubs.
We will create a new module(s) for testing purposes, and we will use the UIAutomator framework to run the tests, common logic can be extracted and shared between tests modules in case we want to parallelize the tests in the future.


## Consequences

The new structure of the project will be as follows:
```
wire-android
├── app
│   ├── ...
├── core
│   ├── ...
├── features
│   ├── ...
├── tests
│   ├── ...
│   ├── testsCore
│   ├── testsSupport
└── build.gradle.kts
```
