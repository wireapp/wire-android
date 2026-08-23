plugins {
    id(libs.plugins.wire.android.library.get().pluginId)
    id(libs.plugins.wire.kover.get().pluginId)
    id(BuildPlugins.junit5)
}

android {
    namespace = "com.wire.android.querymatching"

    buildFeatures {
        compose = false
    }
}

metro {
    enabled.set(false)
    automaticallyAddRuntimeDependencies.set(false)
}

dependencies {
    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.konsist)
    testRuntimeOnly(libs.junit5.engine)
}
