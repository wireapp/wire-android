plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.hilt.android)
    implementation(libs.compose.material3)
}
