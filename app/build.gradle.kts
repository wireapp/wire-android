plugins {
    id(BuildPlugins.androidApplication)
    id(BuildPlugins.kotlinAndroid)
    id(BuildPlugins.kotlinAndroidExtensions)
}

android {
    compileSdkVersion(AndroidSdk.compile)

    defaultConfig {
        applicationId = appId
        minSdkVersion(AndroidSdk.min)
        targetSdkVersion(AndroidSdk.target)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("main") { java.srcDir("src/main/kotlin") }
        getByName("test") { java.srcDir("src/test/kotlin") }
        getByName("androidTest") { java.srcDir("src/androidTest/kotlin") }
    }

    packagingOptions {
        exclude("LICENSE.txt")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/ASL2.0")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE")
    }

    lintOptions {
        isQuiet = true
        isAbortOnError = false
        isIgnoreWarnings = true
        disable("InvalidPackage")           //Some libraries have issues with this.
        disable("OldTargetApi")             //Lint gives this warning related to SDK Beta.
        disable("IconDensities")            //For testing purpose. This is safe to remove.
        disable("IconMissingDensityFolder") //For testing purpose. This is safe to remove.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions("version")
    productFlavors {
        create("dev") {
            setDimension("version")
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("internal") {
            setDimension("version")
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
        }
        create("public") {
            setDimension("version")
        }
    }
}

dependencies {
    // Application dependencies
    implementation(Libraries.kotlinStdLib)
    implementation(Libraries.appCompat)
    implementation(Libraries.ktxCore)
    implementation(Libraries.constraintLayout)

    // Unit/Android tests dependencies
    testImplementation (TestLibraries.junit4)
    testImplementation(TestLibraries.mockito)
    testImplementation(TestLibraries.robolectric)
    testImplementation(TestLibraries.assertJ)

    // Acceptance/Functional tests dependencies
    androidTestImplementation(TestLibraries.testRunner)
    androidTestImplementation(TestLibraries.espresso)
    androidTestImplementation(TestLibraries.testExtJunit)
    androidTestImplementation(TestLibraries.testRules)
    androidTestImplementation(TestLibraries.mockitoAndroid)

    // Development dependencies
    debugImplementation(DevLibraries.leakCanary)
}