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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

private const val COMPOSE_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.plugin.compose"
private const val COMPOSE_STABILITY_CONFIGURATION_FILE = "config/compose-stability.conf"
private const val ENABLE_COMPOSE_COMPILER_REPORTS = "wire.composeCompiler.reports"

/**
 * A convention plugin to apply and configure the JetBrains Compose Compiler Gradle plugin across all modules that use Compose.
 * Compose compiler reports are opt-in to avoid slowing down regular local builds.
 */
class ComposeCompilerConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(COMPOSE_COMPILER_PLUGIN_ID)
        configureComposeCompiler()
    }

    private fun Project.configureComposeCompiler() {
        extensions.configure<ComposeCompilerGradlePluginExtension> {
            stabilityConfigurationFiles.add(
                rootProject.layout.projectDirectory.file(COMPOSE_STABILITY_CONFIGURATION_FILE)
            )

            val reportsEnabled = providers
                .gradleProperty(ENABLE_COMPOSE_COMPILER_REPORTS)
                .map(String::toBoolean)
                .getOrElse(false)

            if (reportsEnabled) {
                metricsDestination.set(layout.buildDirectory.dir("composeCompiler/metrics"))
                reportsDestination.set(layout.buildDirectory.dir("composeCompiler/reports"))
            }
        }
    }
}
