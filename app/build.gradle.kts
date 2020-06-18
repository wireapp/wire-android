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
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets { map { it.java.srcDir("src/${it.name}/kotlin") } }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
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

    // Unit/Android tests dependencies
    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.mockito)
    testImplementation(TestLibraries.robolectric)
    testImplementation(TestLibraries.assertJ)
    testImplementation(TestLibraries.coroutinesTest)

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