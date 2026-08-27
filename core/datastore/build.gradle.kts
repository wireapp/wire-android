plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
}

android {
    namespace = "com.wire.android.datastore"
    // Storage-only module: no Compose here, so the Compose compiler is not needed either.
    buildFeatures {
        compose = false
    }
}

dependencies {
    implementation(project(":core:di"))
    implementation("com.wire.kalium:kalium-logic")
    implementation(libs.androidx.core)
    implementation(libs.androidx.dataStore)
}