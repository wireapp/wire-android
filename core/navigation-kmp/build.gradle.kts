plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.wire.navigation"
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.compose.multiplatform.navigation3.runtime)
                implementation(libs.ktx.serialization)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
