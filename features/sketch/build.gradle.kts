plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
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
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material.core)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.compose.ui.preview)

    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.espresso.core)
}
