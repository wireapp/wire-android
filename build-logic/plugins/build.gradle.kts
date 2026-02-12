/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.serialization)
}

// Configure the build-logic plugins to target JDK 17
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)

    testImplementation(libs.junit4)
    testImplementation(kotlin("test"))
    implementation(libs.ksp.symbol.processing.plugin)
    implementation(libs.ktx.serialization)
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
        register("wireKoverConventionPlugin") {
            id = libs.plugins.wire.kover.get().pluginId
            implementationClass = "KoverConventionPlugin"
        }
        register("appVersionPlugin") {
            id = libs.plugins.wire.versionizer.get().pluginId
            implementationClass = "AppVersionPlugin"
        }
        register("androidTestLibraryConventionPlugin") {
            id = libs.plugins.wire.android.test.library.get().pluginId
            implementationClass = "AndroidTestLibraryConventionPlugin"
        }
        register("androidNavigationConventionPlugin") {
            id = libs.plugins.wire.android.navigation.get().pluginId
            implementationClass = "AndroidNavigationConventionPlugin"
        }
        register("kmpLibraryConventionPlugin") {
            id = libs.plugins.wire.kmp.library.get().pluginId
            implementationClass = "KmpLibraryConventionPlugin"
        }
    }
}
