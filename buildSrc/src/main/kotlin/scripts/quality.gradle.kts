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

package scripts

import findVersion
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import java.util.regex.Pattern

plugins {
    id("com.android.application") apply false
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    val detektVersion = findVersion("detekt").requiredVersion
    detekt("io.gitlab.arturbosch.detekt:detekt-cli:$detektVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:$detektVersion")
    detektPlugins("com.wire:detekt-rules:20260128-162246") {
        isChanging = true
    }
}

// Detekt Configuration
val detektAll by tasks.registering(Detekt::class) {
    dependsOn("enforceUiWaitUtilsUsage") // todo. move later to wire-detekt-rules to enforce it from there.
    group = "Quality"
    description = "Runs a detekt code analysis ruleset on the Wire Android codebase"
    parallel = true
    buildUponDefaultConfig = true

    val outputFile = layout.buildDirectory.file("staticAnalysis/index.html").get()

    setSource(files(rootDir))
    config.setFrom("$rootDir/config/detekt/detekt.yml")

    include("**/*.kt")
    exclude("**/*.kts", "**/build/**", "/buildSrc", "/kalium", "/template")

    baseline.set(file("$rootDir/config/detekt/baseline.xml"))

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(outputFile)
        txt.required.set(false)
    }

    val reportFile = "Static Analysis Report: $outputFile \n"
    doFirst { println(reportFile) }
    doLast { println(reportFile) }
}

tasks.withType(DetektCreateBaselineTask::class) {
    description = "Overrides current baseline."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(files(rootDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/baseline.xml"))

    include("**/*.kt")
    exclude("**/*.kts", "**/build/**", "/buildSrc", "/kalium", "/template")
}

tasks.register("staticCodeAnalysis") {
    description = "Analyses code within the Wire Android codebase"
    dependsOn(detektAll)
}

val enforceUiWaitUtilsUsage by tasks.registering {
    group = "Quality"
    description = "Fails if testsCore uses direct sleep/wait APIs instead of UiWaitUtils."

    doLast {
        val root = rootProject.projectDir
        val targets = fileTree(root.resolve("tests/testsCore/src/androidTest/kotlin")) {
            include("**/*.kt")
        }

        val forbiddenPatterns = listOf(
            Pattern.compile("""\bThread\.sleep\s*\(""") to "Use UiWaitUtils.waitFor(...) or UiWaitUtils.waitForMillis(...)",
            Pattern.compile("""\bSystemClock\.sleep\s*\(""") to "Use UiWaitUtils retry/wait helpers instead of direct sleeps",
            Pattern.compile("""\bwaitForExists\s*\(""") to "Use UiWaitUtils.waitUntilVisibleOrThrow(...) or waitUntilGoneOrThrow(...)",
            Pattern.compile("""\bUiWaitUtils\.WaitUtils\.waitFor\s*\(""") to "Use UiWaitUtils.waitFor(...)",
            Pattern.compile("""import\s+uiautomatorutils\.UiWaitUtils\.WaitUtils\.waitFor""") to "Import UiWaitUtils and call UiWaitUtils.waitFor(...)"
        )

        val violations = mutableListOf<String>()

        targets.files.sortedBy { it.path }.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                forbiddenPatterns.forEach { (pattern, guidance) ->
                    if (pattern.matcher(line).find()) {
                        val relativePath = file.relativeTo(root).path
                        violations += "$relativePath:${index + 1}: $guidance\n    $line"
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("UiWaitUtils policy violations found in testsCore:")
                    appendLine()
                    appendLine(violations.joinToString("\n"))
                }
            )
        }
    }
}

tasks.register("testCoverage") {
    group = "Quality"
    description = "Reports code coverage on tests within the Wire Android codebase."
    dependsOn("koverXmlReportDevDebug")

    val validSubprojects = setOf("core", "features")
    rootProject.subprojects {
        if (name == "app") {
            dependsOn(":app:testDevDebugUnitTest")
        } else if (validSubprojects.contains(parent?.name) &&
            !pluginManager.hasPlugin("com.android.kotlin.multiplatform.library")
        ) {
            dependsOn(":${parent?.name}:$name:testDebugUnitTest")
        }
    }
}
