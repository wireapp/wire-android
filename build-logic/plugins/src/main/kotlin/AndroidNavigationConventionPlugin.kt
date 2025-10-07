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
import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.KspTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class AndroidNavigationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.google.devtools.ksp")
        }

        dependencies {
            add("implementation", project(":core:navigation"))
            add("implementation", findLibrary("compose.navigation"))
            add("implementation", findLibrary("compose.destinations.core")) //compose.destinations.bottom.sheet
            add("implementation", findLibrary("compose.destinations.bottom.sheet"))
            add("ksp", findLibrary("compose.destinations.ksp"))
        }

        // path to the generated navigation annotations
        val dstPath = layout.buildDirectory.dir("generated/navigation").get().asFile
        // clear the directory before generating new files
        dstPath.deleteRecursively()
        // package name suffix for the generated navigation annotations
        val packageNameSuffix = project.path.lowercase().replace(":", ".").replace("[^a-z0-9.]".toRegex(), "")

        // generate a copy of navigation annotations in each module where it's needed and adjust package so that it doesn't conflict
        val copyNavigationAnnotations = tasks.register<Copy>("copyNavigationAnnotations") {
            from(project(":core:navigation").file("src/main/kotlin/com/wire/android/navigation/annotation"))
            into(dstPath)
            filter {
                // adjust the package name in the generated files by adding the suffix so that it doesn't conflict
                // also adjust any imports that reference the annotation package
                when {
                    it.startsWith("package ") -> it + packageNameSuffix
                    it.contains("com.wire.android.navigation.annotation") && it.startsWith("import ") ->
                        it.replace("com.wire.android.navigation.annotation", "com.wire.android.navigation.annotation$packageNameSuffix")
                    else -> it
                }
            }
        }
        // make sure the copy task is executed before the KSP tasks
        target.tasks.withType<KspTask>().configureEach { dependsOn(copyNavigationAnnotations) }

        // configure sourceSets - add generated navigation annotations to each module's source set
        project.kotlinExtension.sourceSets.configureEach { kotlin.srcDir(dstPath) }

        // add the package name suffix to the KSP arguments so that it can be used in the generated code
        project.extensions.getByType<KspExtension>().arg("packageNameSuffix", packageNameSuffix)
    }
}
