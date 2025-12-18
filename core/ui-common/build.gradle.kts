plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    id(BuildPlugins.kotlinParcelize)
    id(BuildPlugins.junit5)
    id(libs.plugins.wire.hilt.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.wire.android.ui.common"
    testFixtures.enable = true
}

dependencies {
    implementation("com.wire.kalium:kalium-logic")
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.ktx.serialization)
    implementation(libs.bundlizer.core)
    implementation(libs.coroutines.android)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.core)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.ui.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.visibilityModifiers)

    // hilt
    implementation(libs.hilt.navigationCompose)
    implementation(libs.hilt.work)

    // smaller view models
    implementation(libs.resaca.core)
    implementation(libs.resaca.hilt)
    implementation(libs.bundlizer.core)

    // Compose Preview
    implementation(libs.compose.edgetoedge.preview)

    // Image loading
    implementation(libs.coil.core)
    implementation(libs.coil.gif)
    implementation(libs.coil.compose)

    implementation(libs.ktx.dateTime)

    // Accompanist
    implementation(libs.accompanist.placeholder)

    testImplementation(libs.junit5.core)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk.core)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)

    testFixturesImplementation(libs.androidx.compose.runtime)
    testFixturesImplementation(libs.coroutines.test)
    testFixturesImplementation(libs.okio.fakeFileSystem)
    testFixturesImplementation("com.wire.kalium:kalium-logic")

}
