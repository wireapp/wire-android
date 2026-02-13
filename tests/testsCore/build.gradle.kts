plugins {
    id(libs.plugins.wire.android.test.library.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    defaultConfig {
        // tell Android to use your custom runner
        testInstrumentationRunner = "com.wire.android.tests.support.suite.TaggedTestRunner"

        // Always apply our JUnit filter for UI tests
        testInstrumentationRunnerArguments["filter"] =
            "com.wire.android.tests.support.suite.TaggedFilter"
    }
    sourceSets {
        getByName("androidTest") {
            kotlin.directories.add("src/androidTest/kotlin")
            kotlin.directories.add(project(":tests:testsSupport").file("src/androidTest/kotlin").path)
            kotlin.directories.add(project(":tests:testsSupport").file("src/main").path)
        }
    }
}
dependencies {
    implementation(libs.androidx.rules)
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.uiAutomator)
    androidTestImplementation(project(":tests:testsSupport"))
    implementation(libs.koin.core)
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit4)
    implementation(libs.datafaker)
    androidTestImplementation(libs.zxing.core)
    androidTestImplementation(libs.zxing.android.embedded)
    androidTestImplementation(libs.gson)
    androidTestImplementation(libs.allure.kotlin.android)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)
}
