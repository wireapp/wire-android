package scripts

import IncludeGitBuildTask
import Libraries

plugins {
    id("com.android.application") apply false
    id("kotlin-android") apply false
}

android {
    packagingOptions {
        pickFirst("META-INF/AL2.0")
        pickFirst("META-INF/LGPL2.1")
        exclude("LICENSE.txt")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/ASL2.0")
        exclude("META-INF/NOTICE")
        exclude("META-INF/licenses/ASM")
    }

    compileOptions {
        // support Java 8 features in API < 26
        // https://developer.android.com/studio/write/java8-support#library-desugaring
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        animationsDisabled = true
    }


    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}

dependencies {
    coreLibraryDesugaring(Libraries.desugaring)
}

project.tasks.register("includeGitBuildIdentifier", IncludeGitBuildTask::class) {
    println("> Task: Registering :includeGitBuildIdentifier")
}

project.afterEvaluate {
    project.tasks.matching { it.name.startsWith("bundle") || it.name.startsWith("assemble") }.configureEach {
        dependsOn("includeGitBuildIdentifier")
    }
}
