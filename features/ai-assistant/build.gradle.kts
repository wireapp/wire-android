plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.hilt.get().pluginId)
    id(BuildPlugins.junit5)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val huggingFaceTokenKey = "HUGGING_FACE_TOKEN"
        val huggingFaceToken: String? = System.getenv(huggingFaceTokenKey) ?: project.getLocalProperty(huggingFaceTokenKey, null)
        buildConfigField("String", huggingFaceTokenKey, huggingFaceToken?.let { "\"$it\"" } ?: "null")

        val huggingFaceBaseUrlKey = "HUGGING_FACE_BASE_URL"
        val huggingFaceBaseUrl: String = System.getenv(huggingFaceBaseUrlKey)
            ?: project.getLocalProperty(huggingFaceBaseUrlKey, "https://huggingface.co")
        buildConfigField("String", huggingFaceBaseUrlKey, "\"$huggingFaceBaseUrl\"")
    }
}

dependencies {
    implementation(projects.core.uiCommon)
    implementation(libs.androidx.core)
    implementation(libs.mediapipe.tasksGenai)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.runtime)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(testFixtures(project(":core:ui-common")))
}
