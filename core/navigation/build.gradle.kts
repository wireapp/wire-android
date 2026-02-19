plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.wire.android.navigation"
}

dependencies {
    implementation(projects.core.uiCommon)
    implementation(libs.visibilityModifiers)
    implementation(libs.compose.navigation)
    implementation(libs.compose.destinations.core)
    implementation(libs.compose.material3)
}
