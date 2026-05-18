plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "com.wire.shared.auth"
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
                implementation(libs.coroutines.test)
            }
        }
    }
}
