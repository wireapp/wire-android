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
    alias(libs.plugins.ksp)
    kotlin(BuildPlugins.kapt)
    alias(libs.plugins.kotlin.serialization)

    id(libs.plugins.aboutLibraries.get().pluginId)

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

dependencies {
    implementation("com.wire.kalium:kalium-logic")
    implementation("com.wire.kalium:kalium-util")

    // Application dependencies
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.dataStore)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.exifInterface)

    implementation(libs.ktx.dateTime)
    implementation(libs.material)
    implementation(libs.coroutines.android)
    implementation(libs.visibilityModifiers)
    implementation(libs.ktx.serialization)
    implementation(libs.ktx.immutableCollections)

    // Image loading
    implementation(libs.coil.core)
    implementation(libs.coil.gif)
    implementation(libs.coil.compose)

    // Androidx - Lifecycle
    implementation(libs.androidx.lifecycle.viewModel)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.lifecycle.liveData)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewModelSavedState)

    // Compose
    implementation(libs.compose.core)
    implementation(libs.compose.foundation)
    // we still cannot get rid of material2 because swipeable is still missing - https://issuetracker.google.com/issues/229839039
    // https://developer.android.com/jetpack/compose/designsystems/material2-material3#components-and
    implementation(libs.compose.material.core)
    implementation(libs.compose.material3)
    // the only libraries with material2 packages that can be used with material3 are icons and ripple
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material.ripple)
    implementation(libs.compose.preview)
    implementation(libs.compose.activity)
    implementation(libs.compose.navigation)
    implementation(libs.compose.constraintLayout)
    implementation(libs.compose.liveData)
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    // Accompanist
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.systemUI)
    implementation(libs.accompanist.placeholder)
    implementation(libs.accompanist.navAnimation)
    implementation(libs.accompanist.indicator)
    implementation(libs.accompanist.flowLayout)

    implementation(libs.androidx.paging3)
    implementation(libs.androidx.paging3Compose)

    // Compose iterative code, layout inspector, etc.
    debugImplementation(libs.compose.tooling)

    // dagger/hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigationCompose)
    implementation(libs.hilt.work)
    kapt(libs.hilt.compiler)

    // smaller view models
    implementation(libs.resaca.core)
    implementation(libs.resaca.hilt)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.fcm)

    implementation(libs.androidx.work)

    // commonMark
    implementation(libs.commonmark.core)
    implementation(libs.commonmark.strikethrough)
    implementation(libs.commonmark.tables)

    implementation(libs.aboutLibraries.core)
    implementation(libs.aboutLibraries.ui)

    // Unit/Android tests dependencies
    testImplementation(libs.androidx.test.archCore)
    testImplementation(libs.junit4) // Maybe migrate completely to Junit 5?
    testImplementation(libs.junit5.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockk.core)
    testImplementation(libs.kluent.core)
    testImplementation(libs.turbine)
    testImplementation(libs.okio.fakeFileSystem)
    testRuntimeOnly(libs.junit5.engine)

    // Acceptance/Functional tests dependencies
    kaptAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.hilt.android)
    androidTestImplementation(libs.hilt.test)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.espresso.accessibility)
    androidTestImplementation(libs.androidx.test.extJunit)
    androidTestImplementation(libs.androidx.test.uiAutomator)
    androidTestImplementation(libs.androidx.test.work)

    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kluent.android)
    androidTestImplementation(libs.compose.test.junit)
    debugImplementation(libs.compose.test.manifest)
    androidTestUtil(libs.androidx.test.orchestrator)

    // Development dependencies
    debugImplementation(libs.leakCanary)

    // Internal, dev, beta and staging only tracking & logging
    devImplementation(libs.dataDog.core)
    internalImplementation(libs.dataDog.core)
    betaImplementation(libs.dataDog.core)
    stagingImplementation(libs.dataDog.core)

    devImplementation(libs.dataDog.compose)
    internalImplementation(libs.dataDog.compose)
    betaImplementation(libs.dataDog.compose)
    stagingImplementation(libs.dataDog.compose)
}
