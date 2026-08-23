plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(BuildPlugins.junit5)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.wire.android.feature.conversation"
    testFixtures.enable = true
}

ksp {
    arg("wire.viewmodelScopedPreview.aggregateName", "ConversationViewModelScopedPreviews")
}

dependencies {
    api(projects.core.uiCommon)
    api("com.wire.kalium:kalium-logic")
    api(libs.androidx.lifecycle.viewModel)
    api(libs.coroutines.android)
    api(libs.ktx.dateTime)
    api(libs.ktx.serialization)

    implementation(projects.core.di)
    implementation(projects.core.search)
    implementation(libs.metrox.viewModelCompose)
    implementation(libs.okio.core)

    implementation(enforcedPlatform(libs.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.compose.material3)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.konsist)
    testImplementation(libs.mockk.core)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(projects.core.uiCommon))
    testRuntimeOnly(libs.junit5.engine)

    testFixturesImplementation("com.wire.kalium:kalium-logic")
    testFixturesImplementation(enforcedPlatform(libs.compose.bom))
    testFixturesImplementation(libs.androidx.compose.runtime)

    ksp(project(":ksp"))
}
