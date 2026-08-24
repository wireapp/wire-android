plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(BuildPlugins.junit5)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.wire.android.feature.authentication"
}

dependencies {
    api(projects.core.navigationKmp)
    implementation(projects.core.uiCommon)
    implementation(libs.ktx.serialization)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.lifecycle.viewModel)

    val composeBom = enforcedPlatform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.activity)
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.preview)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testRuntimeOnly(libs.junit5.engine)
}
