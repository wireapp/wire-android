plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(BuildPlugins.junit5)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
}

android {
    namespace = "com.wire.android.search"
}

dependencies {

    implementation(project(":core:di"))

    implementation("com.wire.kalium:kalium-logic")
    implementation(project(":core:ui-common"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.ktx.serialization)
    implementation(libs.ktx.immutableCollections)
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

    implementation(libs.visibilityModifiers)
    implementation(libs.androidx.paging3)
    implementation(libs.androidx.paging3Compose)
    implementation(libs.resaca.core)
    implementation(libs.compose.edgetoedge.preview)
    implementation(libs.ktx.dateTime)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit5.core)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk.core)
    testImplementation(libs.coroutines.test)
    testImplementation(testFixtures(projects.core.uiCommon))
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.androidx.test.extJunit)
}
