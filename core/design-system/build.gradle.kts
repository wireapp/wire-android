plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.compose.compiler.get().pluginId)
    id(BuildPlugins.junit5)
    alias(libs.plugins.compose.stability.analyzer)
}

android {
    namespace = "com.wire.android.designsystem"
}

metro {
    enabled.set(false)
    automaticallyAddRuntimeDependencies.set(false)
}

dependencies {
    val composeBom = enforcedPlatform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.runtime)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.visibilityModifiers)

    testImplementation(libs.junit5.core)
    testImplementation(libs.konsist)
    testRuntimeOnly(libs.junit5.engine)
}
