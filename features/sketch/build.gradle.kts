plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(BuildPlugins.kotlinParcelize)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":core:ui-common"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.ktx.immutableCollections)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.android)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.core)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.compose.ui.preview)
    implementation(libs.compose.navigation)
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
}

android {
    ksp {
        arg("compose-destinations.moduleName", "sketch")
        arg("compose-destinations.mode", "destinations")
    }
}
