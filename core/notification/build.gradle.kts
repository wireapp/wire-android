plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
}

dependencies {
    implementation(projects.core.media)
    implementation("com.wire.kalium:kalium-common")
    implementation("com.wire.kalium:kalium-data")
    implementation(libs.androidx.core)
    implementation(libs.compose.material3)
}
