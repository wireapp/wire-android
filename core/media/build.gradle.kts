plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.compose.material3)
}
