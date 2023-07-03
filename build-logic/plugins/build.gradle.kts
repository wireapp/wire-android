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
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

// Configure the build-logic plugins to target JDK 11
// TODO: Upgrade Jenkins image to support JDK 17 and bump it here too
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)

    testImplementation(libs.junit4)
    testImplementation(libs.kluent.core)
}

gradlePlugin {
    plugins {
        register("androidLibraryConventionPlugin") {
            id = libs.plugins.wire.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplicationConventionPlugin") {
            id = libs.plugins.wire.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("wireHiltConventionPlugin") {
            id = libs.plugins.wire.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }
    }
}
