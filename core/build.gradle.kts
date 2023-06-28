import com.android.build.api.dsl.AndroidSourceSet

/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
plugins {
    id(BuildPlugins.androidLibrary)
    id(BuildPlugins.kotlinAndroid)

    // TODO: Find alternative so quality and compilation plugins
    //       Can work on both Application and Library
//    id(ScriptPlugins.quality)
//    id(ScriptPlugins.compilation)
    id(ScriptPlugins.spotless)
    id(ScriptPlugins.testing)
}

android {
    // TODO: Move to settings.gradle.kts when upgrading to newer AGP
    compileSdk = AndroidSdk.compile
    namespace = AndroidClient.namespace + ".core"

    // TODO: Centralize some common configs between this and :app
    defaultConfig {
        minSdk = AndroidSdk.min
        targetSdk = AndroidSdk.target
        testInstrumentationRunner = AndroidClient.testRunner
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "clearPackageData" to "true",
                "force-queryable" to "true"
            )
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Libraries.Versions.composeCompiler
    }

    sourceSets {
        map { it.java.srcDir("src/${it.name}/kotlin") }
    }
    // This enables us to share some code between UI and Unit tests!
    fun AndroidSourceSet.includeCommonTestSourceDir() = java {
        srcDir("src/commonTest/kotlin")
    }
    sourceSets["test"].includeCommonTestSourceDir()
    sourceSets["androidTest"].includeCommonTestSourceDir()


    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }

}

dependencies {
    implementation("com.wire.kalium:kalium-logic")
    implementation("com.wire.kalium:kalium-util")

    implementation(Libraries.ktxCore)
    implementation(Libraries.ktxDateTime)
    implementation(Libraries.constraintLayout)
    implementation(Libraries.material)

    implementation(Libraries.Kotlin.coroutinesCore)
    implementation(Libraries.Kotlin.coroutinesAndroid)
    implementation(Libraries.visibilityModifiers)

    // Compose iterative code, layout inspector, etc.
    debugImplementation(Libraries.composeTooling)

    // ViewModel
    implementation(Libraries.Lifecycle.viewModel)
    // ViewModel utilities for Compose
    implementation(Libraries.Lifecycle.viewModelCompose)
    // LiveData
    implementation(Libraries.Lifecycle.liveData)
    // Lifecycles only (without ViewModel or LiveData)
    implementation(Libraries.Lifecycle.runtime)
    // Saved state module for ViewModel
    implementation(Libraries.Lifecycle.viewModelSavedState)

    // Image handling
    implementation(Libraries.coil)
    implementation(Libraries.coilGif)
    implementation(Libraries.coilCompose)

    // Compose
    api(Libraries.composeUi)
    api(Libraries.composeFoundation)
    // we still cannot get rid of material2 because swipeable is still missing - https://issuetracker.google.com/issues/229839039
    // https://developer.android.com/jetpack/compose/designsystems/material2-material3#components-and
    api(Libraries.composeMaterial)
    api(Libraries.composeMaterial3)
    // the only libraries with material2 packages that can be used with material3 are icons and ripple
    api(Libraries.composeMaterialIcons)
    api(Libraries.composeMaterialRipple)
    api(Libraries.composePreview)
    api(Libraries.composeActivity)
    api(Libraries.composeNavigation)
    api(Libraries.composeConstraintLayout)

    // Accompanist
    api(Libraries.accompanistPager)
    api(Libraries.accompanistSystemUI)
    api(Libraries.accompanistPlaceholder)
    api(Libraries.accompanistNavAnimation)
    api(Libraries.accompanistIndicator)
    api(Libraries.composeRuntimeLiveData)
    api(Libraries.accompanistFlowLayout)
}
