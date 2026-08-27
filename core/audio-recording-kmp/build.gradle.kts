plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
}

kotlin {
    android {
        namespace = "com.wire.media.recording"
        withHostTest {}
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core:content-kmp"))
                api(project(":core:media-player-kmp"))
                api("com.wire.kalium:kalium-logic")
                api(libs.coroutines.core)
                api(libs.okio.core)
                implementation(libs.jetbrains.compose.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
                implementation(libs.okio.fakeFileSystem)
            }
        }
    }
}
