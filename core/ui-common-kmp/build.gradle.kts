plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    android {
        namespace = "com.wire.android.ui.common"
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.jetbrains.compose.runtime)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.ui)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.core)
            }
        }
    }
}
