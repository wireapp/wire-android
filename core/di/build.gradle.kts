plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    id(BuildPlugins.junit5)
    alias(libs.plugins.compose.stability.analyzer)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.compose.activity)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.metrox.viewModelCompose)
    implementation(libs.compose.material3)
    implementation(libs.resaca.core)
    implementation(libs.resaca.metro)

    testImplementation(libs.junit5.core)
    testRuntimeOnly(libs.junit5.engine)
}
