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

    // Internal Script plugins
    id(ScriptPlugins.variants)
    id(ScriptPlugins.quality)
    id(ScriptPlugins.compilation)
    id(ScriptPlugins.testing)
    id(ScriptPlugins.spotless)
    id(BuildPlugins.gms)
}

repositories {
    google()
}

android {
    compileSdk = AndroidSdk.compile

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
            force(Libraries.Kotlin.coroutinesCore)
            force(Libraries.Kotlin.coroutinesAndroid)
        }
    }
}

dependencies {
    implementation("com.wire.kalium:kalium-logic")

    // Application dependencies
    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.appCompat)
    implementation(Libraries.ktxCore)
    implementation(Libraries.ktxDateTime)
    implementation(Libraries.constraintLayout)
    implementation(Libraries.material)
    implementation(Libraries.Kotlin.coroutinesCore)
    implementation(Libraries.Kotlin.coroutinesAndroid)
    implementation(Libraries.visibilityModifiers)
    implementation(Libraries.browser)
    implementation(Libraries.dataStore)
    implementation(Libraries.splashscreen)
    implementation(Libraries.exifInterface)
    implementation(Libraries.Kotlin.serialization)
    implementation(Libraries.ktxImmutableCollections)

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
    implementation(Libraries.composeUi)
    implementation(Libraries.composeFoundation)
    implementation(Libraries.composeMaterial3)
    implementation(Libraries.composeMaterial)
    implementation(Libraries.composePreview)
    implementation(Libraries.composeIcons)
    implementation(Libraries.composeActivity)
    implementation(Libraries.composeNavigation)
    implementation(Libraries.composeConstraintLayout)
    implementation(Libraries.accompanistPager)
    implementation(Libraries.accompanistSystemUI)
    implementation(Libraries.accompanistPlaceholder)
    implementation(Libraries.accompanistNavAnimation)
    implementation(Libraries.accompanistIndicator)
    implementation(Libraries.composeRuntimeLiveData)
    implementation(Libraries.accompanistFlowLayout)

    implementation(Libraries.Paging.runtime)
    implementation(Libraries.Paging.compose)

    // Compose iterative code, layout inspector, etc.
    debugImplementation(Libraries.composeTooling)

    // dagger/hilt
    implementation(Libraries.Hilt.android)
    implementation(Libraries.Hilt.navigationCompose)
    kapt(Libraries.Hilt.compiler)

    // firebase
    implementation(platform(Libraries.Firebase.firebaseBOM))
    implementation(Libraries.Firebase.firebaseCloudMessaging)

    implementation(Libraries.workManager)

    // TODO: remove or move to Libraries
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.2")

    // Unit/Android tests dependencies
    testImplementation(TestLibraries.androidCore)
    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.robolectric)
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

    // Internal only tracking & logging

    implementation(Libraries.dataDog)

}
