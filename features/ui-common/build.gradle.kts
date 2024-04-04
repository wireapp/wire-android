plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
}

android {
    namespace = "com.wire.android.ui.common"
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.core)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)

    implementation(libs.accompanist.systemUI)
    implementation(libs.visibilityModifiers)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
}
