plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.hilt.get().pluginId)
    id(BuildPlugins.junit5)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.core.uiCommon)
    implementation(libs.androidx.core)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.runtime)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(testFixtures(project(":core:ui-common")))
}
