package scripts

plugins { id("core.android") }



android {
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
            dimension = "version"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("internal") {
            dimension = "version"
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
        }
        create("public") {
            dimension = "version"
        }
    }
}