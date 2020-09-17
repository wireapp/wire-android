package scripts

plugins { id("core.android") }

android {
    packagingOptions {
        exclude("LICENSE.txt")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/ASL2.0")
        exclude("META-INF/NOTICE")
        exclude("META-INF/licenses/ASM")
    }

    compileOptions {
        // support Java 8 features in API < 26
        // https://developer.android.com/studio/write/java8-support#library-desugaring
        coreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    coreLibraryDesugaring(Libraries.desugaring)
}
