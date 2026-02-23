plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.hilt.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.core.di)
    implementation("com.wire.kalium:kalium-logic")
    implementation("com.wire.kalium:kalium-nomaddevice")
    implementation("com.wire.kalium:kalium-userstorage")
    implementation("com.wire.kalium:kalium-hooks")
    implementation(libs.androidx.startup)
    implementation(libs.androidx.compose.runtime)
}
