/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

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
    println("> Registering Task :includeGitBuildIdentifier")
}

project.afterEvaluate {
    project.tasks.matching { it.name.startsWith("bundle") || it.name.startsWith("assemble") }.configureEach {
        dependsOn("includeGitBuildIdentifier")
    }
}
