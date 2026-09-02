plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(BuildPlugins.junit5)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.wire.android.audioplayer"
}

dependencies {

    implementation(project(":core:di"))
    implementation(project(":core:ui-common"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.coroutines.android)

    val composeBom = enforcedPlatform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.compose.ui.preview)
    implementation(libs.metrox.viewModelCompose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk.core)
    testImplementation(libs.turbine)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(testFixtures(project(":core:ui-common")))
    ksp(project(":ksp"))
}
