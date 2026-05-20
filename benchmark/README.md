## App Benchmarks

This module now uses `com.android.test` and targets `:app` directly. Gradle builds the tested app APK and the benchmark test APK together, installs both on the connected device, and runs the selected benchmark. You do not need to manually assemble and install the app APK first.

## Prerequisites

- Use a real Android device. Do not use an emulator for macrobenchmarks.
- Connect exactly one Android device.
- If running a login benchmark, make sure the benchmark account can still register a device.
- The current setup is aimed at the `prod` flavor with the `benchmark` build type.

## Run a benchmark

Run the startup benchmark without login:

```shell
./gradlew :benchmark:connectedProdBenchmarkBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.wire.benchmark.StartupBenchmark#startUpWithoutBaselineProfiler \
  -Pandroid.testInstrumentationRunnerArguments.TARGET_PACKAGE="com.wire"
```

Run the startup benchmark with login:

```shell
./gradlew :benchmark:connectedProdBenchmarkBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.wire.benchmark.StartupBenchmarkWithLogin#startUpWithoutBaselineProfiler \
  -Pandroid.testInstrumentationRunnerArguments.TARGET_PACKAGE="com.wire" \
  -Pandroid.testInstrumentationRunnerArguments.EMAIL="$EMAIL" \
  -Pandroid.testInstrumentationRunnerArguments.PASSWORD="$PASSWORD"
```

Run the baseline profile generator:

```shell
./gradlew :benchmark:connectedProdBenchmarkBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.wire.benchmark.BaselineGenerator \
  -Pandroid.testInstrumentationRunnerArguments.TARGET_PACKAGE="com.wire" \
  -Pandroid.testInstrumentationRunnerArguments.EMAIL="$EMAIL" \
  -Pandroid.testInstrumentationRunnerArguments.PASSWORD="$PASSWORD"
```

## Notes

- The benchmark module reads `TARGET_PACKAGE`, `EMAIL`, and `PASSWORD` from instrumentation runner arguments.
- The app APK is produced from `:app` automatically through `targetProjectPath = ":app"`.
- Benchmark output is written under `benchmark/build/outputs/connected_android_test_additional_output/`.
- The committed pre-profile reference snapshot lives under `benchmark/baselines/`.
