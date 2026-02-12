plugins {
    id(libs.plugins.wire.kmp.library.get().pluginId)
    alias(libs.plugins.jetbrains.compose)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.core)
            }
        }
    }
}

android {
    namespace = "com.wire.android.ui.common"
}
