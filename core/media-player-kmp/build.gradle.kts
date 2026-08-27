plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
}

kotlin {
    android {
        namespace = "com.wire.media.player"
        withHostTest {}
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.coroutines.core)
                api(libs.okio.core)
                implementation(libs.jetbrains.compose.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }
    }
}
