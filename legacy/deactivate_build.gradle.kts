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

    externalNativeBuild {
        cmake {
            version = AndroidNdk.cMakeVersion
        }
        ndkBuild {
            ndkVersion = AndroidNdk.version
            path(File("src/main/jni/Android.mk"))
        }
    }

    sourceSets {
        map { it.java.srcDir("src/${it.name}/kotlin") }
    }
    fun AndroidSourceSet.includeCommonTestSourceDir() = java {
        srcDir("src/commonTest/kotlin")
    }
    sourceSets["test"].includeCommonTestSourceDir()
    sourceSets["androidTest"].includeCommonTestSourceDir()

    configurations.implementation.configure {
        exclude(module = "protobuf-java")
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
//    implementation(Libraries.Koin.workManager)
    implementation(Libraries.Kotlin.coroutinesCore)
    implementation(Libraries.Kotlin.coroutinesAndroid)
    implementation(Libraries.pinEditText)
    implementation(Libraries.viewPager2)
    implementation(Libraries.paging)
    implementation(Libraries.glide)
    implementation(Libraries.fragment)
    kapt(Libraries.glideCompiler)
    implementation(Libraries.workManager)
    implementation(Libraries.scarlet)
    implementation(Libraries.scarletOkhttp)
    implementation(Libraries.scarletLifecycle)
    implementation(Libraries.scarletGson)

    implementation(Libraries.messageProto)
    implementation(Libraries.Crypto.cryptobox)

    implementation(Libraries.Retrofit.core)
    implementation(Libraries.Retrofit.gsonConverter)
    implementation(Libraries.Retrofit.protoConverter)
    implementation(Libraries.okHttpLogging)

    kapt(Libraries.Room.sqlLiteJdbc)
    implementation(Libraries.Room.runtime)
    implementation(Libraries.Room.ktx)
    kapt(Libraries.Room.compiler)

    implementation("com.github.poovamraj:PinEditTextField:1.2.6")

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
