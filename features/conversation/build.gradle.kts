plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(BuildPlugins.junit5)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    alias(libs.plugins.compose.stability.analyzer)
}

android {
    namespace = "com.wire.android.feature.conversation"
}

dependencies {
    implementation(enforcedPlatform(libs.compose.bom))
    implementation(libs.androidx.compose.runtime)

    testImplementation(libs.junit5.core)
    testImplementation(libs.konsist)
    testRuntimeOnly(libs.junit5.engine)
}
