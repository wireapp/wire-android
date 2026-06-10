plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    id(BuildPlugins.kotlinParcelize)
    id(BuildPlugins.junit5)
    alias(libs.plugins.ksp)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
}

android {
    namespace = "com.wire.android.ui.common"
    testFixtures.enable = true
}

dependencies {
    implementation(project(":core:di"))

    implementation("com.wire.kalium:kalium-logic")
    implementation("com.wire.kalium:kalium-util")
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.ktx.serialization)
    implementation(libs.bundlizer.core)
    implementation(libs.coroutines.android)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.ui.preview)
    implementation(libs.metrox.viewModelCompose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.visibilityModifiers)

    implementation(libs.androidx.paging3)
    implementation(libs.androidx.paging3Compose)

    // smaller view models
    implementation(libs.resaca.core)
    implementation(libs.bundlizer.core)

    // Compose Preview
    implementation(libs.compose.edgetoedge.preview)

    // Image loading
    implementation(libs.coil.core)
    implementation(libs.coil.gif)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    implementation(libs.ktx.dateTime)

    // Accompanist
    implementation(libs.accompanist.placeholder)

    implementation(projects.ksp)
    ksp(project(":ksp"))

    testImplementation(libs.junit5.core)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)

    testFixturesImplementation(libs.androidx.compose.runtime)
    testFixturesImplementation(libs.coroutines.test)
    testFixturesImplementation(libs.okio.fakeFileSystem)
    testFixturesImplementation("com.wire.kalium:kalium-logic")
    testFixturesImplementation(libs.junit5.core)
    testFixturesImplementation(libs.mockk.core)

}
