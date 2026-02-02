## App Benchmarks

This is the benchmark project for the Android App. To run the benchmarks, the following prerequisites must be met:

- A real Android device is used for testing (emulators are not supported).
- There is a beta benchmark build of the app installed on the device. 
- If using the test with login, make sure the max amount of devices registered to the account is not exceeded.

### Building the benchmark APK
To build the benchmark APK, use the following command:

```shell
./gradlew clean assembleBetaBenchmark
```

### Running the benchmarks
To run the benchmarks, use the following command:
```shell
./gradlew :benchmark:connectedDebugAndroidTest
```

Alternatively, you can run the benchmarks directly from Android Studio by selecting the `benchmark` module and running the `connectedDebugAndroidTest` configuration.
