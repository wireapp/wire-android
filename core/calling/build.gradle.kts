plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(BuildPlugins.junit5)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
}

android {
    namespace = "com.wire.android.calling"
}

metro {
    enabled.set(false)
    automaticallyAddRuntimeDependencies.set(false)
}

dependencies {
    api(projects.core.uiCommon)
    implementation("com.wire.kalium:kalium-common")
    api("com.wire.kalium:kalium-logic")
    api(libs.coroutines.android)
    implementation(libs.androidx.core)
    implementation(libs.visibilityModifiers)

    implementation(enforcedPlatform(libs.compose.bom))
    implementation(libs.androidx.compose.runtime)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk.core)
    testImplementation(libs.turbine)
    testImplementation(libs.konsist)
    testImplementation(testFixtures(projects.core.uiCommon))
    testRuntimeOnly(libs.junit5.engine)
}
