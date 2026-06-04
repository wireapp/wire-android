plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.compose.activity)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigationCompose)
    implementation(libs.compose.material3)
}
