plugins {
    id(BuildPlugins.androidLibrary)
    id(BuildPlugins.kotlinAndroid)
}

android {
    compileSdkVersion(AndroidSdk.compile)

    defaultConfig {
        minSdkVersion(AndroidSdk.min)
        targetSdkVersion(AndroidSdk.target)
        versionCode = AndroidClient.versionCode
        versionName = AndroidClient.versionName
        testInstrumentationRunner = AndroidClient.testRunner
    }

    sourceSets { map { it.java.srcDir("src/${it.name}/kotlin") } }

    externalNativeBuild {
        cmake {
            version = AndroidNdk.cMakeVersion
        }
        ndkBuild {
            ndkVersion = AndroidNdk.version
            path(File("src/main/jni/Android.mk"))
        }
    }

}
/** TODO Add Sonatype repos once CryptoBox is published
repositories {
    maven(Repositories.sonatypeReleases)
    maven(Repositories.sonatypeSnapshots)
}
**/

dependencies {

    implementation(Libraries.Kotlin.stdLib)
    implementation(Libraries.appCompat)
    implementation(Libraries.ktxCore)
    implementation(Libraries.kotPref)

    implementation(Libraries.Crypto.cryptobox)
    implementation(Libraries.Crypto.libSodium)

    testImplementation(TestLibraries.androidCore)
    testImplementation(TestLibraries.junit4)
    testImplementation(TestLibraries.robolectric)
    testImplementation(TestLibraries.coroutinesTest)
    testImplementation(TestLibraries.testCore)
    testImplementation(TestLibraries.koinTest)
    testImplementation(TestLibraries.mockk)
    testImplementation(TestLibraries.kluent)

    androidTestImplementation(TestLibraries.testRunner)
    androidTestImplementation(TestLibraries.Espresso.core)
    androidTestImplementation(TestLibraries.testExtJunit)
    androidTestImplementation(TestLibraries.mockkAndroid)
    androidTestImplementation(TestLibraries.kluentAndroid)
}
