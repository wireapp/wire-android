plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    android {
        namespace = "com.wire.wireone"
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
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "WireOneKmp"
            isStatic = false
        }
    }
}
