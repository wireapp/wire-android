import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import com.wire.android.gradle.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

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
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
        }

        extensions.configure<ApplicationExtension> {
            // TODO: Handle flavors
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = AndroidSdk.target

            packagingOptions {
                resources {
                    pickFirsts.add("META-INF/AL2.0")
                    pickFirsts.add("META-INF/LGPL2.1")
                    excludes.add("LICENSE.txt")
                    excludes.add("META-INF/DEPENDENCIES")
                    excludes.add("META-INF/ASL2.0")
                    excludes.add("META-INF/NOTICE")
                    excludes.add("META-INF/licenses/ASM")
                }
            }

            testOptions {
                animationsDisabled = true
            }
        }
    }
}
