const val appId = "com.wire.android"

object AndroidSdk {
    const val min = 24
    const val compile = 30
    const val target = compile
}

object AndroidClient {
    const val appId = "com.wire.android"
    const val versionCode = 1
    const val versionName = "1.0"
    const val testRunner = "androidx.test.runner.AndroidJUnitRunner"
}

object BuildPlugins {
    object Versions {
        const val gradleVersion = "6.6.1"
    }

    const val androidApplication = "com.android.application"
    const val kotlinAndroid = "kotlin-android"
    const val kotlinAndroidExtensions = "kotlin-android-extensions"
    const val kotlinKapt = "kotlin-kapt"
}

object ScriptPlugins {
    const val infrastructure = "scripts.infrastructure"
    const val variants = "scripts.variants"
    const val quality = "scripts.quality"
    const val compilation = "scripts.compilation"
}

object Libraries {
    object Versions {
        const val kotlin = "1.4.10"
        const val coroutines = "1.3.9"
        const val jetpack = "1.1.0"
        const val constraintLayout = "1.1.3"
        const val ktx = "1.3.0"
        const val material = "1.1.0"
        const val koin = "2.1.6"
        const val lifecycleKtx = "2.2.0"
        const val retrofit = "2.9.0"
        const val okHttpLogging = "4.7.2"
        const val pinEditText = "1.2.3"
        const val viewPager2 = "1.0.0"
        const val room = "2.2.5"
        const val desugaring = "1.0.10"
    }

    const val appCompat        = "androidx.appcompat:appcompat:${Versions.jetpack}"
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
    const val ktxCore          = "androidx.core:core-ktx:${Versions.ktx}"
    const val material         = "com.google.android.material:material:${Versions.material}"
    const val livedataKtx      = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycleKtx}"
    const val viewModelKtx     = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycleKtx}"
    const val okHttpLogging    = "com.squareup.okhttp3:logging-interceptor:${Versions.okHttpLogging}"
    const val pinEditText      = "com.poovam:pin-edittext-field:${Versions.pinEditText}"
    const val viewPager2       = "androidx.viewpager2:viewpager2:${Versions.viewPager2}"
    const val desugaring       = "com.android.tools:desugar_jdk_libs:${Versions.desugaring}"

    object Koin {
        const val androidCore  = "org.koin:koin-android:${Versions.koin}"
        const val viewModel    = "org.koin:koin-android-viewmodel:${Versions.koin}"
    }

    object Kotlin {
        const val stdLib            = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
        const val coroutinesCore    = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    }

    object Retrofit {
        const val core          = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
        const val gsonConverter = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    }

    object Room {
        const val runtime  = "androidx.room:room-runtime:${Versions.room}"
        const val ktx      = "androidx.room:room-ktx:${Versions.room}"
        const val compiler = "androidx.room:room-compiler:${Versions.room}"
    }
}

object TestLibraries {
    private object Versions {
        const val junit4 = "4.13"
        const val mockito = "3.3.0"
        const val mockk = "1.10.2"
        const val kluent = "1.60"
        const val robolectric = "4.4"
        const val assertJ = "3.16.1"
        const val testRunner = "1.2.0"
        const val espresso = "3.2.0"
        const val testExtensions = "1.1.1"
        const val testRules = "1.2.0"
        const val uiAutomator = "2.2.0"
        const val testCore = "2.1.0"
    }

    object Espresso {
        const val core                = "androidx.test.espresso:espresso-core:${Versions.espresso}"
        const val intents             = "androidx.test.espresso:espresso-intents:${Versions.espresso}"
        //Androidx isn't support yet: https://github.com/android/android-test/issues/492
        const val accessibility = "com.android.support.test.espresso:espresso-accessibility:${Versions.espresso}"
    }

    const val junit4         = "junit:junit:${Versions.junit4}"
    const val robolectric    = "org.robolectric:robolectric:${Versions.robolectric}"
    const val assertJ        = "org.assertj:assertj-core:${Versions.assertJ}"
    const val testRunner     = "androidx.test:runner:${Versions.testRunner}"
    const val testExtJunit   = "androidx.test.ext:junit:${Versions.testExtensions}"
    const val testRules      = "androidx.test:rules:${Versions.testRules}"
    const val mockitoAndroid = "org.mockito:mockito-android:${Versions.mockito}"
    const val uiAutomator    = "androidx.test.uiautomator:uiautomator:${Versions.uiAutomator}"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Libraries.Versions.coroutines}"
    const val testCore       = "androidx.arch.core:core-testing:${Versions.testCore}"
    const val koinTest       = "org.koin:koin-test:${Libraries.Versions.koin}"

    const val mockk           = "io.mockk:mockk:${Versions.mockk}"
    const val kluent          = "org.amshove.kluent:kluent:${Versions.kluent}"
}


object DevLibraries {
    private object Versions {
        const val leakCanary = "2.3"
        const val fragment = "1.2.5"
    }
    const val fragmentTesting = "androidx.fragment:fragment-testing:${Versions.fragment}"
    const val leakCanary      = "com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}"
}
