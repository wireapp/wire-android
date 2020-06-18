plugins {
    // Application Specific plugins
    id(BuildPlugins.androidApplication)
    id(BuildPlugins.kotlinAndroid)
    id(BuildPlugins.kotlinAndroidExtensions)

    // Internal Script plugins
    id(ScriptPlugins.variants)
    id(ScriptPlugins.quality)
    id(ScriptPlugins.compilation)
}

android {
    compileSdkVersion(AndroidSdk.compile)

    defaultConfig {
        applicationId = appId
        minSdkVersion(AndroidSdk.min)
        targetSdkVersion(AndroidSdk.target)
        versionCode = AndroidSdk.versionCode
        versionName = AndroidSdk.versionName
        testInstrumentationRunner = AndroidSdk.testInstrumentationRunner
    }

    sourceSets { map { it.java.srcDir("src/${it.name}/kotlin") } }
}

dependencies {
    // Application dependencies
    implementation(Libraries.kotlinStdLib)
    implementation(Libraries.appCompat)
    implementation(Libraries.ktxCore)
    implementation(Libraries.constraintLayout)
    implementation(Libraries.material)
    implementation(Libraries.livedataKtx)
    implementation(Libraries.Koin.androidCore)
    implementation(Libraries.Koin.viewModel)

    // Unit/Android tests dependencies
    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.mockito)
    testImplementation(TestLibraries.robolectric)
    testImplementation(TestLibraries.assertJ)

    // Acceptance/Functional tests dependencies
    androidTestImplementation(TestLibraries.testRunner)
    androidTestImplementation(TestLibraries.espresso)
    androidTestImplementation(TestLibraries.testExtJunit)
    androidTestImplementation(TestLibraries.testRules)
    androidTestImplementation(TestLibraries.mockitoAndroid)
    androidTestImplementation(TestLibraries.uiAutomator)

    // Development dependencies
    debugImplementation(DevLibraries.leakCanary)
}