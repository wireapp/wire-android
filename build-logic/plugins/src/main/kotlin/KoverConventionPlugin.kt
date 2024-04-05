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
import kotlinx.kover.gradle.plugin.dsl.KoverReportExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<KoverReportExtension> {
                defaults {
                    if (project.name == "app") {
                        println(">> Configuring Kover devDebug variant for: ${project.name}")
                        mergeWith("devDebug")
                    } else {
                        println(">> Configuring Kover debug variant for: ${project.name}")
                        mergeWith("debug")
                    }

                    filters {
                        excludes {
                            classes(
                                "*Fragment",
                                "*Fragment\$*",
                                "*Activity",
                                "*Activity\$*",
                                "*.databinding.*",
                                "*.BuildConfig",
                                "**/R.class",
                                "**/R\$*.class",
                                "**/Manifest*.*",
                                "**/Manifest$*.class",
                                "**/*Test*.*",
                                "*NavArgs*",
                                "*ComposableSingletons*",
                                "*_HiltModules*",
                                "*Hilt_*",
                            )
                            packages(
                                "hilt_aggregated_deps",
                                "com.wire.android.di",
                                "dagger.hilt.internal.aggregatedroot.codegen",
                                "com.wire.android.ui.home.conversations.mock",
                            )
                            annotatedBy(
                                "*Generated*",
                                "*HomeNavGraph*",
                                "*Destination*",
                                "*Composable*",
                                "*Preview*",
                            )
                        }
                    }
                }
            }
        }
    }
}
