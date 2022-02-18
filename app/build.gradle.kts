import com.android.build.api.dsl.AndroidSourceSet

plugins {
    // Application Specific plugins
    id(BuildPlugins.androidApplication)
    id(BuildPlugins.kotlinAndroid)
    //id(BuildPlugins.kotlinAndroidExtensions)
    id(BuildPlugins.kotlinKapt)
    id(BuildPlugins.hilt)
    kotlin(BuildPlugins.kapt)

    // Internal Script plugins
    id(ScriptPlugins.variants)
    id(ScriptPlugins.quality)
    id(ScriptPlugins.compilation)
    id(ScriptPlugins.testing)
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
        versionName = "v${AndroidClient.versionName}(${versionCode})"
        testInstrumentationRunner = AndroidClient.testRunner
        setProperty("archivesBaseName", "${applicationId}-v${versionName}(${versionCode})")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Libraries.Versions.compose
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
    //configurations.implementation.configure {
    //    exclude(module = "protobuf-java")
    //}
}

kapt {
    correctErrorTypes = true
}


dependencies {
    implementation("com.wire.kalium:kalium-logic")

    // Application dependencies
    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.appCompat)
    implementation(Libraries.ktxCore)
    implementation(Libraries.constraintLayout)
    implementation(Libraries.material)
    implementation(Libraries.livedataKtx)
    implementation(Libraries.viewModelKtx)
    implementation(Libraries.Kotlin.coroutinesCore)
    implementation(Libraries.Kotlin.coroutinesAndroid)
    implementation(Libraries.viewPager2)
    implementation(Libraries.visibilityModifiers)
    implementation(Libraries.browser)
    implementation(Libraries.dataStore)
    implementation(Libraries.splashscreen)

    // lifecycle
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

    //Compose
    implementation(Libraries.composeUi)
    implementation(Libraries.composeMaterial3)
    implementation(Libraries.composeMaterial)
    implementation(Libraries.composeTooling)
    implementation(Libraries.composeIcons)
    implementation(Libraries.composeActivity)
    implementation(Libraries.composeNavigation)
    implementation(Libraries.composeConstraintLayout)
    implementation(Libraries.accompanistPager)
    implementation(Libraries.accompanistSystemUI)
    implementation(Libraries.accompanistPlaceholder)
    implementation(Libraries.composeRuntimeLiveData)

    // dagger/hilt
    implementation(Libraries.Hilt.android)
    implementation(Libraries.Hilt.navigationCompose)
    kapt(Libraries.Hilt.compiler)

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

    // Development dependencies
    //debugImplementation(DevLibraries.fragmentTesting)
    debugImplementation(DevLibraries.leakCanary)
}
