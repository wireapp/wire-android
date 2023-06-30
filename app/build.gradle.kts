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
 *
 *
 */

import com.android.build.api.dsl.AndroidSourceSet

plugins {
    // Application Specific plugins
    id(BuildPlugins.androidApplication)
    id(BuildPlugins.kotlinAndroid)
    // id(BuildPlugins.kotlinAndroidExtensions)
    id(BuildPlugins.kotlinKapt)
    id(BuildPlugins.kotlinParcelize)
    id(BuildPlugins.hilt)
    id(BuildPlugins.junit5)
    kotlin(BuildPlugins.kapt)
    kotlin(BuildPlugins.serialization) version Libraries.Versions.kotlin

    id(ScriptPlugins.aboutLibraries)
    // Internal Script plugins
    id(ScriptPlugins.variants)
    id(ScriptPlugins.quality)
    id(ScriptPlugins.compilation)
    id(ScriptPlugins.testing)
    id(ScriptPlugins.spotless)
}

repositories {
    mavenLocal()
    wireDetektRulesRepo()
    google()
}

android {
    compileSdk = AndroidSdk.compile
    namespace = AndroidClient.namespace

    defaultConfig {
        applicationId = AndroidClient.appId
        minSdk = AndroidSdk.min
        targetSdk = AndroidSdk.target
        versionCode = AndroidClient.versionCode
        versionName = "v${AndroidClient.versionName}($versionCode)"
        testInstrumentationRunner = AndroidClient.testRunner
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "clearPackageData" to "true",
                "force-queryable" to "true"
            )
        )
        setProperty("archivesBaseName", "$applicationId-v$versionName($versionCode)")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = findVersion("compose.compiler").requiredVersion
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

    // Remove protobuf-java as dependencies, so we can get protobuf-lite
    // configurations.implementation.configure {
    //    exclude(module = "protobuf-java")
    // }

    packagingOptions {
        resources.pickFirsts.add("google/protobuf/*.proto")
        jniLibs.pickFirsts.add("**/libsodium.so")
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

kapt {
    correctErrorTypes = true
}

configurations {
    all {
        resolutionStrategy {
            // Force dependencies to resolve coroutines versions to native-mt variant
            force(deps.coroutines.android)
        }
    }
}

dependencies {
    implementation("com.wire.kalium:kalium-logic")
    implementation("com.wire.kalium:kalium-util")

    // Application dependencies
    implementation(deps.androidx.appcompat)
    implementation(deps.androidx.core)
    implementation(deps.ktx.dateTime)
    implementation(deps.material)
    implementation(deps.coroutines.android)
    implementation(deps.visibilityModifiers)
    implementation(deps.androidx.browser)
    implementation(Libraries.dataStore)
    implementation(Libraries.splashscreen)
    implementation(Libraries.exifInterface)
    implementation(deps.ktx.serialization)
    implementation(deps.ktx.immutableCollections)

    // Image handling
    implementation(Libraries.coil)
    implementation(Libraries.coilGif)
    implementation(Libraries.coilCompose)

    /** lifecycle **/
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

    // Compose
    implementation(deps.compose.core)
    implementation(deps.compose.foundation)
    // we still cannot get rid of material2 because swipeable is still missing - https://issuetracker.google.com/issues/229839039
    // https://developer.android.com/jetpack/compose/designsystems/material2-material3#components-and
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    // the only libraries with material2 packages that can be used with material3 are icons and ripple
    implementation(deps.compose.ripple)
    implementation(deps.compose.icons)
    implementation(deps.compose.preview)
    implementation(deps.compose.activity)
    implementation(deps.compose.navigation)
    implementation(Libraries.composeConstraintLayout)
    implementation(Libraries.accompanistPager)
    implementation(Libraries.accompanistSystemUI)
    implementation(Libraries.accompanistPlaceholder)
    implementation(Libraries.accompanistNavAnimation)
    implementation(Libraries.accompanistIndicator)
    implementation(Libraries.composeRuntimeLiveData)
    implementation(Libraries.accompanistFlowLayout)

    implementation(deps.androidx.paging3)
    implementation(deps.androidx.paging3Compose)

    // Compose iterative code, layout inspector, etc.
    debugImplementation(deps.compose.tooling)

    // dagger/hilt
    implementation(Libraries.Hilt.android)
    implementation(Libraries.Hilt.navigationCompose)
    kapt(Libraries.Hilt.compiler)

    // smaller view models
    implementation(deps.resaca.core)
    implementation(deps.resaca.hilt)

    // firebase
    implementation(platform(Libraries.Firebase.firebaseBOM))
    implementation(Libraries.Firebase.firebaseCloudMessaging)

    implementation(deps.androidx.work)

    // commonMark
    implementation(Libraries.CommonMark.core)
    implementation(Libraries.CommonMark.strikethrough)
    implementation(Libraries.CommonMark.tables)

    implementation(Libraries.aboutLibraries.core)
    implementation(Libraries.aboutLibraries.ui)

    // Unit/Android tests dependencies
    testImplementation(TestLibraries.androidCore)
    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.coroutinesTest)
    testImplementation(TestLibraries.testCore)
    testImplementation(TestLibraries.mockk)
    testImplementation(TestLibraries.kluent)
    testImplementation(TestLibraries.junit5)
    testImplementation(TestLibraries.turbine)
    testImplementation(TestLibraries.okio)
    testRuntimeOnly(TestLibraries.junit5Engine)

    // Acceptance/Functional tests dependencies
    androidTestImplementation(TestLibraries.testRunner)
    androidTestImplementation(TestLibraries.Espresso.core)
    androidTestImplementation(TestLibraries.Espresso.intents)
    androidTestImplementation(TestLibraries.Espresso.accessibility)
    androidTestImplementation(TestLibraries.testExtJunit)
    androidTestImplementation(TestLibraries.testRules)
    androidTestImplementation(TestLibraries.uiAutomator)
    androidTestImplementation(TestLibraries.coroutinesTest)
    androidTestImplementation(TestLibraries.mockkAndroid)
    androidTestImplementation(TestLibraries.kluentAndroid)
    androidTestImplementation(TestLibraries.composeJunit)
    debugImplementation(TestLibraries.composeManifest)
    androidTestImplementation(Libraries.Hilt.android)
    androidTestImplementation(Libraries.Hilt.hiltTest)
    androidTestImplementation(TestLibraries.workManager)
    kaptAndroidTest(Libraries.Hilt.compiler)
    androidTestUtil(TestLibraries.testOrchestrator)

    implementation(Libraries.Hilt.hiltWork)

    // Development dependencies
    debugImplementation(DevLibraries.leakCanary)

    // Internal, dev, beta and staging only tracking & logging
    devImplementation(Libraries.dataDog)
    internalImplementation(Libraries.dataDog)
    betaImplementation(Libraries.dataDog)
    stagingImplementation(Libraries.dataDog)

    devImplementation(Libraries.dataDogCompose)
    internalImplementation(Libraries.dataDogCompose)
    betaImplementation(Libraries.dataDogCompose)
    stagingImplementation(Libraries.dataDogCompose)
}
