plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)

    testImplementation(libs.junit4)
}
