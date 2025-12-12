plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(BuildPlugins.kotlinParcelize)
    id(BuildPlugins.junit5)
    alias(libs.plugins.ksp)
    id(libs.plugins.wire.android.navigation.get().pluginId)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":core:ui-common"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.ktx.immutableCollections)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.core)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.compose.ui.preview)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk.core)
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
}

android {
    ksp {
        arg("compose-destinations.moduleName", "sketch")
        arg("compose-destinations.mode", "destinations")
    }
}
