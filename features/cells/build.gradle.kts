plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.hilt.get().pluginId)
    id(BuildPlugins.kotlinParcelize)
    id(BuildPlugins.junit5)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation("com.wire.kalium:kalium-common")
    implementation("com.wire.kalium:kalium-logic")
    implementation("com.wire.kalium:kalium-cells")
    implementation(project(":core:ui-common"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.ktx.immutableCollections)

    // hilt
    implementation(libs.hilt.navigationCompose)
    implementation(libs.hilt.work)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.core)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.compose.ui.preview)
    implementation(libs.compose.navigation)
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    implementation(libs.coil.core)
    implementation(libs.coil.gif)
    implementation(libs.coil.video)
    implementation(libs.coil.compose)

    implementation(libs.ktx.dateTime)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk.core)
    testImplementation(libs.turbine)
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
}

android {
    ksp {
        arg("compose-destinations.moduleName", "cells")
        arg("compose-destinations.mode", "destinations")
    }
}
