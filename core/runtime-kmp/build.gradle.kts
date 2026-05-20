plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
}

kotlin {
    android {
        namespace = "com.wire.android.runtime"
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
