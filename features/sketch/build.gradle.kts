plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.wire.android.feature.sketch"
    android.buildFeatures.viewBinding = true
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.runtime.liveData)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
}
