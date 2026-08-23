/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.architecture

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModuleDependencyBoundaryTest {

    @Test
    fun everyFeatureIsInboundFromAppExactlyOnceThroughTheFeatureConvention() {
        val appBuildScript = appBuildScript.readText()
        assertTrue(
            featureModules.isNotEmpty(),
            "No active direct feature modules were discovered from features/*/build.gradle.kts.",
        )
        assertTrue(
            settingsBuildScript.readText().contains("it.name != \"template\""),
            "Feature discovery must stay aligned with settings.gradle.kts excluding the template directory.",
        )

        featureModules.forEach { feature ->
            val edge = Regex(
                """implementationWithCoverage\s*\(\s*projects\s*\.\s*features\s*\.\s*${feature.accessor}\s*\)""",
            )
            assertEquals(
                1,
                edge.findAll(appBuildScript).count(),
                ":app must declare exactly one implementationWithCoverage(projects.features.${feature.accessor}) edge for :features:${feature.name}.",
            )
        }
    }

    @Test
    fun featuresDoNotDependOnAppOrOtherFeatures() {
        featureModules.forEach { feature ->
            val buildScript = feature.buildScript.readText()
            assertFalse(
                appProjectEdge.containsMatchIn(buildScript),
                ":features:${feature.name} must not declare a project dependency on :app in ${feature.buildScript.relativeTo(repoRoot)}.",
            )
            assertFalse(
                featureProjectEdge.containsMatchIn(buildScript),
                ":features:${feature.name} must not declare a project dependency on :features:* in ${feature.buildScript.relativeTo(repoRoot)}; extract shared contracts to neutral core/platform ownership.",
            )
        }
    }

    @Test
    fun graphDocumentationExistsAndEmbedsTheCanonicalMermaidBodyVerbatim() {
        assertTrue(graphMarkdown.isFile, "Missing docs/architecture/android-module-graph.md.")
        assertTrue(graphMermaid.isFile, "Missing docs/architecture/android-module-graph.mmd.")

        val embeddedBody = mermaidBlock.find(graphMarkdown.readText())?.groupValues?.get(1)
        assertTrue(
            embeddedBody != null,
            "android-module-graph.md must contain a Mermaid block with the canonical target graph.",
        )
        assertEquals(
            graphMermaid.readText().trimEnd(),
            embeddedBody!!.trimEnd(),
            "The Mermaid block in android-module-graph.md must match android-module-graph.mmd verbatim.",
        )
    }

    private data class FeatureModule(
        val name: String,
        val accessor: String,
        val buildScript: File,
    )

    private companion object {
        val repoRoot: File = findRepoRoot()
        val settingsBuildScript = File(repoRoot, "settings.gradle.kts")
        val appBuildScript = File(repoRoot, "app/build.gradle.kts")
        val graphMarkdown = File(repoRoot, "docs/architecture/android-module-graph.md")
        val graphMermaid = File(repoRoot, "docs/architecture/android-module-graph.mmd")
        val settingsExcludedFeatureDirectories = setOf("template")
        val featureModules: List<FeatureModule> = File(repoRoot, "features")
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter(File::isDirectory)
            // settings.gradle.kts excludes direct-child templates from project inclusion.
            .filter { it.name !in settingsExcludedFeatureDirectories }
            .map { directory -> FeatureModule(directory.name, directory.name.toTypeSafeAccessor(), File(directory, "build.gradle.kts")) }
            .filter { it.buildScript.isFile }
            .sortedBy(FeatureModule::name)
            .toList()

        val appProjectEdge = Regex(
            """projects\s*\.\s*app\b|project\s*\(\s*(?:path\s*=\s*)?["']\s*:\s*app\s*["']""",
        )
        val featureProjectEdge = Regex(
            """projects\s*\.\s*features\s*\.|project\s*\(\s*(?:path\s*=\s*)?["']\s*:\s*features\s*:""",
        )
        val mermaidBlock = Regex("""```mermaid\n([\s\S]*?)\n```""")

        fun findRepoRoot(): File {
            var candidate = File(System.getProperty("user.dir")).absoluteFile
            while (true) {
                if (File(candidate, "settings.gradle.kts").isFile) return candidate
                val parent = candidate.parentFile ?: break
                candidate = parent
            }
            error("Could not locate repository root from user.dir=${System.getProperty("user.dir")}; settings.gradle.kts is required.")
        }

        fun String.toTypeSafeAccessor(): String =
            split('-').mapIndexed { index, segment ->
                if (index == 0) segment else segment.replaceFirstChar(Char::uppercaseChar)
            }.joinToString(separator = "")
    }
}
