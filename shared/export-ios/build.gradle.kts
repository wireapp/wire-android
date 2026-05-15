plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "com.wire.ios.shared"
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.coroutines.core)
                implementation("com.wire.kalium:kalium-logic")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "WireIosShared"
            isStatic = false
        }
    }
}
