plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
}

android {
    namespace = "com.wire.android.navigation"
    buildTypes {
        create("benchmark") {
        }
    }
}

dependencies {
    implementation(libs.visibilityModifiers)
    implementation(libs.compose.navigation)
    implementation(libs.compose.destinations.core)
}
