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

import io.gitlab.arturbosch.detekt.Detekt
import scripts.Variants_gradle.Default

plugins {
    id("com.android.application") apply false
    id("jacoco")
    id("io.gitlab.arturbosch.detekt")
}

// Lint Configuration
android {
    lintOptions {
        isQuiet = true
        isAbortOnError = false
        isIgnoreWarnings = true
        disable("InvalidPackage") // Some libraries have issues with this.
        disable("OldTargetApi") // Lint gives this warning related to SDK Beta.
        disable("IconDensities") // For testing purpose. This is safe to remove.
        disable("IconMissingDensityFolder") // For testing purpose. This is safe to remove.
    }
}

// Detekt Configuration
val detektAll by tasks.registering(Detekt::class) {
    group = "Quality"
    description = "Runs a detekt code analysis ruleset on the Wire Android codebase "
    parallel = true
    buildUponDefaultConfig = true

    val outputFile = "${project.buildDir}/staticAnalysis/index.html"

    setSource(files(project.projectDir))
    config.setFrom("${project.rootDir}/config/detekt/detekt.yml")

    include("**/*.kt")
    exclude("**/*.kts", "*/build/*", "/buildSrc")

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(file(outputFile))
        txt.required.set(false)
    }

    val reportFile = "Static Analysis Report: $outputFile \n"
    doFirst { println(reportFile) }
    doLast { println(reportFile) }
}

tasks.register("staticCodeAnalysis") {
    description = "Analyses code within the Wire Android codebase"
    dependsOn(detektAll)
}

// Jacoco Configuration
val jacocoReport by tasks.registering(JacocoReport::class) {
    group = "Quality"
    description = "Reports code coverage on tests within the Wire Android codebase"
    dependsOn("test${Default.BUILD_VARIANT}UnitTest")

    val outputDir = "$buildDir/jacoco/html"
    val classPathBuildVariant = "${Default.BUILD_FLAVOR}${Default.BUILD_TYPE.capitalize()}"

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(file(outputDir))
    }

    classDirectories.setFrom(
        fileTree(project.buildDir) {
            include(
                "**/classes/**/main/**",
                "**/intermediates/classes/$classPathBuildVariant/**",
                "**/intermediates/javac/$classPathBuildVariant/*/classes/**",
                "**/tmp/kotlin-classes/$classPathBuildVariant/**"
            )
            exclude(
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/Manifest$*.class",
                "**/*Test*.*",
                "**/Injector.*",
                "android/**/*.*",
                "**/*\$Lambda$*.*",
                "**/*\$inlined$*.*",
                "**/di/*.*",
                "**/*Database.*",
                "**/*Response.*",
                "**/*Application.*",
                "**/*Entity.*",
                "**/*Screen.*",
                "**/mock/**",
                "**/theme/**/*.*", // Ignores jetpack compose theme related code
                "**/common/**/*.*", // Ignores jetpack compose common components related code
                "**/navigation/**/*.*", // Ignores jetpack navigation related code
            )
        }
    )

    sourceDirectories.setFrom(
        fileTree(project.projectDir) {
            include("src/main/java/**", "src/main/kotlin/**")
        }
    )

    executionData.setFrom(
        fileTree(project.buildDir) {
            include("**/*.exec", "**/*.ec")
        }
    )

    doLast { println("Report file: $outputDir/index.html") }
}

tasks.register("testCoverage") {
    group = "Quality"
    description = "Reports code coverage on tests within the Wire Android codebase."
    dependsOn(jacocoReport)
}
