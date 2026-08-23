plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    id(BuildPlugins.junit5)
    alias(libs.plugins.compose.stability.analyzer)
}

android {
    namespace = "com.wire.android.navigation"
}

dependencies {
    api(projects.core.navigationKmp)
    implementation(projects.core.designSystem)
    implementation(projects.core.uiCommon)
    implementation(libs.visibilityModifiers)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.compose.multiplatform.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewModelNavigation3)

    testImplementation(libs.junit4)
    testImplementation(libs.junit5.core)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.vintage.engine)
}
