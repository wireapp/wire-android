plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    android {
        namespace = "com.wire.wireone"
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.ui)
                implementation(projects.core.uiCommonKmp)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("com.wire.kalium:kalium-logic")
            }
        }

        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "WireOneKmp"
            isStatic = false
        }
    }
}

tasks.register("wireoneWebRun") {
    group = "wireone"
    description = "Run WireOne Web (Wasm) development server."
    dependsOn("wasmJsBrowserDevelopmentRun")
}

tasks.register("wireoneWebBuild") {
    group = "wireone"
    description = "Build WireOne Web (Wasm) browser distribution."
    dependsOn("wasmJsBrowserDistribution")
}

configurations.matching { config ->
    config.name.contains("ios", ignoreCase = true)
}.configureEach {
    // TODO(KMP): remove after kalium datetime constraint is aligned with Compose MPP stack.
    resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
}
