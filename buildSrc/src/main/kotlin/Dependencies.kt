const val appId = "com.wire.android"

object AndroidSdk {
    const val min = 24
    const val compile = 33
    const val target = compile
}

object AndroidNdk {
    const val version = "22.1.7171670"
    const val cMakeVersion = "3.18.1"
}

object AndroidClient {
    const val appId = "com.wire.android"
    val versionCode = Versionizer().versionCode
    const val versionName = "0.0.1"
    const val testRunner = "com.wire.android.HiltAwareTestRunner"
}

object BuildPlugins {
    object Versions {
        const val gradleVersion = "7.5.1"
    }

    const val androidApplication = "com.android.application"
    const val androidLibrary = "com.android.library"
    const val kotlinAndroid = "kotlin-android"
    const val kotlinAndroidExtensions = "kotlin-android-extensions"
    const val kotlinKapt = "kotlin-kapt"
    const val kotlinParcelize = "kotlin-parcelize"
    const val kapt = "kapt"
    const val hilt = "dagger.hilt.android.plugin"
    const val junit5 = "de.mannodermaus.android-junit5"
    const val gms = "com.google.gms.google-services"
    const val serialization = "plugin.serialization"
}

object ScriptPlugins {
    const val infrastructure = "scripts.infrastructure"
    const val variants = "scripts.variants"
    const val quality = "scripts.quality"
    const val compilation = "scripts.compilation"
    const val testing = "scripts.testing"
    const val spotless = "scripts.spotless"
}

object Repositories {
    const val sonatypeReleases = "https://oss.sonatype.org/content/repositories/releases"
    const val sonatypeSnapshots = "https://oss.sonatype.org/content/repositories/snapshots"
}

object Libraries {
    object Versions {
        const val kotlin = "1.7.10"
        const val coroutines = "1.6.1-native-mt"
        const val jetpack = "1.1.0"
        const val constraintLayout = "1.1.3"
        const val ktx = "1.8.0"
        const val material = "1.5.0"
        const val pinEditText = "1.2.3"
        const val sqlLiteJdbc = "3.36.0"
        const val desugaring = "1.1.5"
        const val workManager = "2.7.1"
        const val fragment = "1.5.0"
        const val compose = "1.3.1"
        const val composeMaterial = compose
        const val composeMaterial3 = "1.0.0-alpha11"
        const val composeActivity = "1.4.0"
        const val composeNavigation = "2.4.2"
        const val accompanist = "0.27.0"
        const val composeConstraint = "1.0.0"
        const val hilt = "2.43.2"
        const val hiltWork = "1.0.0"
        const val lifecycle = "2.5.0"
        const val visibilityModifiers = "1.1.0"
        const val composeHiltNavigation = "1.0.0"
        const val browser = "1.3.0"
        const val dataStore = "1.0.0"
        const val paging3 = "3.1.1"
        const val paging3Compose = "1.0.0-alpha15"
        const val splashscreen = "1.0.0-beta01"
        const val coil = "2.2.1"
        const val exif = "1.3.3"
        const val firebaseBOM = "29.3.1"
        const val dataDog = "1.14.1"
        const val ktxDateTime = "0.4.0"
        const val ktxSerialization = "1.3.2"
        const val ktxImmutableCollections = "0.3.5"
    }

    // AndroidX Dependencies
    const val appCompat                 = "androidx.appcompat:appcompat:${Versions.jetpack}"
    const val constraintLayout          = "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
    const val ktxCore                   = "androidx.core:core-ktx:${Versions.ktx}"
    const val workManager               = "androidx.work:work-runtime-ktx:${Versions.workManager}"
    const val fragment                  = "androidx.fragment:fragment:${Versions.fragment}"
    const val composeUi                 = "androidx.compose.ui:ui:${Versions.compose}"
    const val composeFoundation         = "androidx.compose.foundation:foundation:${Versions.compose}"
    const val composeMaterial           = "androidx.compose.material:material:${Versions.composeMaterial}"
    const val composeMaterial3          = "androidx.compose.material3:material3:${Versions.composeMaterial3}"
    const val composeTooling            = "androidx.compose.ui:ui-tooling:${Versions.compose}"
    const val composePreview            = "androidx.compose.ui:ui-tooling-preview:${Versions.compose}"
    const val composeActivity           = "androidx.activity:activity-compose:${Versions.composeActivity}"
    const val composeIcons              = "androidx.compose.material:material-icons-extended:${Versions.compose}"
    const val composeNavigation         = "androidx.navigation:navigation-compose:${Versions.composeNavigation}"
    const val composeConstraintLayout   = "androidx.constraintlayout:constraintlayout-compose:${Versions.composeConstraint}"
    const val composeRuntimeLiveData    = "androidx.compose.runtime:runtime-livedata:${Versions.compose}"
    const val dataStore                 = "androidx.datastore:datastore-preferences:${Versions.dataStore}"
    const val exifInterface             = "androidx.exifinterface:exifinterface:${Versions.exif}"
    const val ktxDateTime               = "org.jetbrains.kotlinx:kotlinx-datetime:${Versions.ktxDateTime}"
    const val ktxImmutableCollections   = "org.jetbrains.kotlinx:kotlinx-collections-immutable:${Versions.ktxImmutableCollections}"

    // Other dependencies
    const val desugaring                = "com.android.tools:desugar_jdk_libs:${Versions.desugaring}"
    const val accompanistPager          = "com.google.accompanist:accompanist-pager:${Versions.accompanist}"
    const val accompanistSystemUI       = "com.google.accompanist:accompanist-systemuicontroller:${Versions.accompanist}"
    const val accompanistPlaceholder    = "com.google.accompanist:accompanist-placeholder:${Versions.accompanist}"
    const val accompanistNavAnimation   = "com.google.accompanist:accompanist-navigation-animation:${Versions.accompanist}"
    const val accompanistIndicator      = "com.google.accompanist:accompanist-pager-indicators:${Versions.accompanist}"
    const val accompanistFlowLayout     = "com.google.accompanist:accompanist-flowlayout:${Versions.accompanist}"
    const val material                  = "com.google.android.material:material:${Versions.material}"
    const val visibilityModifiers       = "io.github.esentsov:kotlin-visibility:${Versions.visibilityModifiers}"
    const val browser                   = "androidx.browser:browser:${Versions.browser}"
    const val splashscreen              = "androidx.core:core-splashscreen:${Versions.splashscreen}"
    const val coil                      = "io.coil-kt:coil:${Versions.coil}"
    const val coilGif                   = "io.coil-kt:coil-gif:${Versions.coil}"
    const val coilCompose               = "io.coil-kt:coil-compose:${Versions.coil}"
    const val dataDog                   = "com.datadoghq:dd-sdk-android:${Versions.dataDog}"

    object Hilt {
        const val android = "com.google.dagger:hilt-android:${Versions.hilt}"
        const val compiler = "com.google.dagger:hilt-android-compiler:${Versions.hilt}"
        const val gradlePlugin = "com.google.dagger:hilt-android-gradle-plugin:${Versions.hilt}"
        const val navigationCompose = "androidx.hilt:hilt-navigation-compose:${Versions.composeHiltNavigation}"
        const val hiltTest = "com.google.dagger:hilt-android-testing:${Versions.hilt}"
        const val hiltWork = "androidx.hilt:hilt-work:${Versions.hiltWork}"
    }

    object Lifecycle {
        const val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
        const val viewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycle}"
        const val liveData = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}"
        const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"
        const val viewModelSavedState = "androidx.lifecycle:lifecycle-viewmodel-savedstate:${Versions.lifecycle}"
    }

    object Kotlin {
        const val stdLib            = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
        const val coroutinesCore    = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
        const val serialization     = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.ktxSerialization}"
        const val playServices     = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${Versions.ktxSerialization}"
    }

    object Firebase {
        const val firebaseBOM = "com.google.firebase:firebase-bom:${Versions.firebaseBOM}"
        const val firebaseCloudMessaging = "com.google.firebase:firebase-messaging-ktx"
    }

    object Paging {
        const val runtime = "androidx.paging:paging-runtime:${Versions.paging3}"
        const val compose = "androidx.paging:paging-compose:${Versions.paging3Compose}"
    }
}

object TestLibraries {

    private object Versions {
        const val androidCore = "1.4.0"
        const val junit4 = "4.13"
        const val junit5 = "5.8.2"
        const val mockk = "1.12.3"
        const val kluent = "1.68"
        const val robolectric = "4.5.1"
        const val testRunner = "1.4.0"
        const val testOrchestrator = "1.4.1"
        const val espresso = "3.4.0"
        const val testExtensions = "1.1.1"
        const val testRules = "1.4.0"
        const val uiAutomator = "2.2.0"
        const val testCore = "2.1.0"
        const val turbine = "0.7.0"
        const val okio = "3.2.0"
    }

    object Espresso {
        const val core                = "androidx.test.espresso:espresso-core:${Versions.espresso}"
        const val intents             = "androidx.test.espresso:espresso-intents:${Versions.espresso}"
        //Androidx isn't support yet: https://github.com/android/android-test/issues/492
        const val accessibility       = "com.android.support.test.espresso:espresso-accessibility:${Versions.espresso}"
    }

    const val junit4            = "junit:junit:${Versions.junit4}"
    const val junit5            = "org.junit.jupiter:junit-jupiter-api:${Versions.junit5}"
    const val junit5Engine      = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit5}"
    const val robolectric       = "org.robolectric:robolectric:${Versions.robolectric}"
    const val testRunner        = "androidx.test:runner:${Versions.testRunner}"
    const val testOrchestrator  = "androidx.test:orchestrator:${Versions.testOrchestrator}"
    const val testExtJunit      = "androidx.test.ext:junit:${Versions.testExtensions}"
    const val testRules         = "androidx.test:rules:${Versions.testRules}"
    const val uiAutomator       = "androidx.test.uiautomator:uiautomator:${Versions.uiAutomator}"
    const val coroutinesTest    = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Libraries.Versions.coroutines}"
    const val testCore          = "androidx.arch.core:core-testing:${Versions.testCore}"
    const val androidCore       = "androidx.test:core:${Versions.androidCore}"
    const val mockk             = "io.mockk:mockk:${Versions.mockk}"
    const val mockkAndroid      = "io.mockk:mockk-android:${Versions.mockk}"
    const val kluent            = "org.amshove.kluent:kluent:${Versions.kluent}"
    const val kluentAndroid     = "org.amshove.kluent:kluent-android:${Versions.kluent}"
    const val workManager       = "androidx.work:work-testing:${Libraries.Versions.workManager}"
    const val turbine           = "app.cash.turbine:turbine:${Versions.turbine}"
    const val okio              = "com.squareup.okio:okio-fakefilesystem:${Versions.okio}"

    // Test rules and transitive dependencies:
    const val composeJunit      = "androidx.compose.ui:ui-test-junit4:${Libraries.Versions.compose}"
    // Needed for createComposeRule, but not createAndroidComposeRule:
    const val composeManifest   = "androidx.compose.ui:ui-test-manifest:${Libraries.Versions.compose}"
}

object DevLibraries {
    private object Versions {
        const val leakCanary = "2.7"
        const val fragment = "1.4.0"
    }
    const val fragmentTesting = "androidx.fragment:fragment-testing:${Versions.fragment}"
    const val leakCanary      = "com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}"
}
