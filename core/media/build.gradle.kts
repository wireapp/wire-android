plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.hilt.android)
    implementation(libs.compose.material3)
}
