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
    namespace = "com.wire.android.feature.conversation.folders"
}

ksp {
    arg("wire.viewmodelScopedPreview.aggregateName", "ConversationFoldersViewModelScopedPreviews")
}

dependencies {
    api(projects.core.uiCommon)
    api("com.wire.kalium:kalium-logic")
    api(libs.androidx.lifecycle.viewModel)
    api(libs.coroutines.android)
    api(libs.ktx.immutableCollections)
    api(libs.ktx.serialization)

    api(enforcedPlatform(libs.compose.bom))
    api(libs.androidx.compose.runtime)
    api("androidx.compose.foundation:foundation")

    implementation(projects.core.di)
    implementation(libs.metrox.viewModelCompose)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.konsist)
    testImplementation(libs.mockk.core)
    testImplementation(libs.turbine)
    testImplementation(testFixtures(projects.core.uiCommon))
    testRuntimeOnly(libs.junit5.engine)

    ksp(project(":ksp"))
}
