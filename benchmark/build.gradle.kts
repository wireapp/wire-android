/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
import flavor.FlavorDimensions
import flavor.ProductFlavors

plugins {
    id("com.android.test")
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.wire.benchmark"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Required because :tests:testsSupport depends on java.time APIs.
        isCoreLibraryDesugaringEnabled = true
    }

    flavorDimensions += FlavorDimensions.DEFAULT
    productFlavors {
        ProductFlavors.all.forEach { flavor ->
            create(flavor.buildName) {
                dimension = flavor.dimensions
            }
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        // For baseline profile generation targeting prodCompatrelease.
        // The benchmark module itself doesn't need minification (it's not shipped).
        // What matters is that the app it targets is minified correctly.
        create("compatrelease") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    sourceSets {
        getByName("main") {
            kotlin.directories.add(project(":tests:testsSupport").file("src/main").path)
        }
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.extJunit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.test.uiAutomator)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.datafaker)
    implementation(libs.gson)
    implementation(libs.junit4)
    implementation(libs.zxing.android.embedded)
    implementation(libs.zxing.core)
    implementation(project(":tests:testsSupport"))
    coreLibraryDesugaring(libs.android.desugarJdkLibs)
}
