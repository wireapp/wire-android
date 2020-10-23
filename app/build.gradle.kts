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
}

android {
    compileSdkVersion(AndroidSdk.compile)

    defaultConfig {
        applicationId = AndroidClient.appId
        minSdkVersion(AndroidSdk.min)
        targetSdkVersion(AndroidSdk.target)
        versionCode = AndroidClient.versionCode
        versionName = AndroidClient.versionName
        testInstrumentationRunner = AndroidClient.testRunner
    }

    testOptions {
        animationsDisabled = true
    }

    sourceSets { map { it.java.srcDir("src/${it.name}/kotlin") } }
}

dependencies {
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
    implementation(Libraries.pinEditText)
    implementation(Libraries.viewPager2)

    implementation(Libraries.Retrofit.core)
    implementation(Libraries.Retrofit.gsonConverter)
    implementation(Libraries.okHttpLogging)

    implementation(Libraries.Room.runtime)
    implementation(Libraries.Room.ktx)
    kapt(Libraries.Room.compiler)

    // Unit/Android tests dependencies
    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.mockito)
    testImplementation(TestLibraries.robolectric)
    testImplementation(TestLibraries.assertJ)
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
    androidTestImplementation(TestLibraries.mockitoAndroid) {
        exclude(module = "mockito-core")
    }
    androidTestImplementation(TestLibraries.uiAutomator)
    androidTestImplementation(TestLibraries.assertJ)
    androidTestImplementation(TestLibraries.coroutinesTest)

    // Development dependencies
    debugImplementation(DevLibraries.fragmentTesting)
    debugImplementation(DevLibraries.leakCanary)
}