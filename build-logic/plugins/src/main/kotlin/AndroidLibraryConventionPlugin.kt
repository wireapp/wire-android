import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.gradle.LibraryExtension
import com.wire.android.gradle.configureCompose
import com.wire.android.gradle.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.project

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
class AndroidLibraryConventionPlugin: Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager){
            apply("com.android.library")
            apply("org.jetbrains.kotlin.android")
        }

        extensions.configure<LibraryExtension> {
            // TODO: Handle flavors
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = AndroidSdk.target

            configureCompose(this)

            // Replace with official test fixtures
            // This enables us to share some code between UI and Unit tests!
            fun AndroidSourceSet.includeCommonTestSourceDir() = java {
                srcDir("src/commonTest/kotlin")
            }
            sourceSets["test"].includeCommonTestSourceDir()
            sourceSets["androidTest"].includeCommonTestSourceDir()

            testOptions {
                execution = "ANDROIDX_TEST_ORCHESTRATOR"
                animationsDisabled = true
                unitTests.isReturnDefaultValues = true
                unitTests.isIncludeAndroidResources = true
            }
        }

        dependencies {
            add("implementation", project(":core:ui"))
        }
    }
}
