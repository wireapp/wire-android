plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    id(BuildPlugins.junit5)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.wire.android.ui.common"
}

dependencies {
    implementation("com.wire.kalium:kalium-logic")
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.ktx.serialization)
    implementation(libs.bundlizer.core)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.core)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.visibilityModifiers)

    // Compose Preview
    implementation(libs.compose.edgetoedge.preview)

    // Image loading
    implementation(libs.coil.core)
    implementation(libs.coil.gif)
    implementation(libs.coil.compose)

    implementation(libs.ktx.dateTime)

    testImplementation(libs.junit5.core)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk.core)
    testImplementation(libs.kluent.core)
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
}
