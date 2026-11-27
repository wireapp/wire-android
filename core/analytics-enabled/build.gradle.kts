plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(projects.core.uiCommon)

    implementation("com.wire.kalium:kalium-data")
    implementation("com.wire.kalium:kalium-logic")
    api(project(":core:analytics"))

    val composeBom = enforcedPlatform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)

    implementation(libs.countly.sdk)

    testImplementation(libs.junit4)
    testImplementation(libs.mockk.core)
    testImplementation(libs.coroutines.test)
}
