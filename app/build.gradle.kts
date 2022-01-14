import com.android.build.api.dsl.AndroidSourceSet

plugins {
    // Application Specific plugins
    id(BuildPlugins.androidApplication)
    id(BuildPlugins.kotlinAndroid)
    id(BuildPlugins.kotlinAndroidExtensions)
    id(BuildPlugins.kotlinKapt)

    // Internal Script plugins
    id(ScriptPlugins.variants)
    id(ScriptPlugins.quality)
    id(ScriptPlugins.compilation)
    id(ScriptPlugins.testing)
}

android {
    compileSdkVersion(AndroidSdk.compile)

    defaultConfig {
        applicationId = AndroidClient.appId
        minSdkVersion(AndroidSdk.min)
        targetSdkVersion(AndroidSdk.target)
        versionCode = AndroidClient.versionCode
        versionName = "v${AndroidClient.versionName}(${versionCode})"
        testInstrumentationRunner = AndroidClient.testRunner
        setProperty("archivesBaseName", "${applicationId}-v${versionName}(${versionCode})")

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
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
    configurations.implementation.configure {
        exclude(module = "protobuf-java")
    }
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
    implementation(Libraries.Koin.androidCore)
    implementation(Libraries.Koin.viewModel)
    implementation(Libraries.Kotlin.coroutinesCore)
    implementation(Libraries.Kotlin.coroutinesAndroid)
    implementation(Libraries.viewPager2)
    implementation(Libraries.activityCompose)
    implementation(Libraries.composeMaterial)
    implementation(Libraries.composeLiveData)
    implementation(Libraries.composeIcons)
    implementation(Libraries.composeTooling)
    implementation(Libraries.accompanistPager)
    implementation(Libraries.accompanistSystemUI)

    // Unit/Android tests dependencies
    testImplementation(TestLibraries.androidCore)
    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.robolectric)
    testImplementation(TestLibraries.coroutinesTest)
    testImplementation(TestLibraries.testCore)
    testImplementation(TestLibraries.koinTest)
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
    debugImplementation(DevLibraries.fragmentTesting)
    debugImplementation(DevLibraries.leakCanary)
}
