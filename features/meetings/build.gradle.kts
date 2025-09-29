plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(libs.plugins.wire.hilt.get().pluginId)
    id(BuildPlugins.kotlinParcelize)
    id(BuildPlugins.junit5)
    alias(libs.plugins.ksp)
    id(libs.plugins.wire.android.navigation.get().pluginId)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation("com.wire.kalium:kalium-common")
    implementation("com.wire.kalium:kalium-logic")
    implementation(project(":core:ui-common"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.ktx.immutableCollections)
    implementation(libs.ktx.serialization)

    // hilt
    implementation(libs.hilt.navigationCompose)
    implementation(libs.hilt.work)

    // smaller view models
    implementation(libs.resaca.core)
    implementation(libs.resaca.hilt)
    implementation(libs.bundlizer.core)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.core)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.compose.ui.preview)

    implementation(libs.ktx.dateTime)

    implementation(libs.androidx.paging3)
    implementation(libs.androidx.paging3Compose)

    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk.core)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.paging.testing)
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
}

android {
    ksp {
        arg("compose-destinations.moduleName", "meetings")
        arg("compose-destinations.mode", "destinations")
    }
}
