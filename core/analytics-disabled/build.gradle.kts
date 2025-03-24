plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)

    api(project(":core:analytics"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)

    testImplementation(libs.junit4)
}
