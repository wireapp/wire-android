plugins {
    id(libs.plugins.wire.android.test.library.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    sourceSets {
        getByName("androidTest") {
            kotlin.srcDirs("src/androidTest/kotlin")
            kotlin.srcDirs(project(":tests:testsSupport").file("src/androidTest/kotlin"))
        }
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.uiAutomator)
    androidTestImplementation(project(":tests:testsSupport"))
}
