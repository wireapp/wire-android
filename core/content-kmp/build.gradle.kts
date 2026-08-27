plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
}

kotlin {
    android {
        namespace = "com.wire.content"
        withHostTest {}
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("com.wire.kalium:kalium-data")
                api(libs.okio.core)
                implementation(libs.jetbrains.compose.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
