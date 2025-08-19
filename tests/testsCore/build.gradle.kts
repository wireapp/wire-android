plugins {
    id(libs.plugins.wire.android.test.library.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    sourceSets {
        getByName("androidTest") {
            kotlin.srcDirs("src/androidTest/kotlin")
            kotlin.srcDirs(project(":tests:testsSupport").file("src/androidTest/kotlin"))
            kotlin.srcDirs(project(":tests:testsSupport").file("src/main"))
        }
    }
}
    dependencies {
        val composeBom = platform(libs.compose.bom)
        implementation(composeBom)
        implementation(libs.compose.ui)

        //implementation("net.datafaker:datafaker:2.4.1")
        androidTestImplementation(libs.androidx.test.runner)
        androidTestImplementation(libs.androidx.test.extJunit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(libs.androidx.test.uiAutomator)
        androidTestImplementation(project(":tests:testsSupport"))
        implementation(libs.koin.core)
        androidTestImplementation(libs.koin.test)
        androidTestImplementation(libs.koin.test.junit4)
        implementation(libs.datafaker)
        implementation("com.google.zxing:core:3.5.2")
        implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    }

