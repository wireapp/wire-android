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
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    id("com.android.application") apply false
    id("jacoco")
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    val detektVersion = "1.19.0"
    detekt("io.gitlab.arturbosch.detekt:detekt-cli:$detektVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
    detektPlugins("com.wire:detekt-rules:1.0.0-SNAPSHOT") {
        isChanging = true
    }
}

// Detekt Configuration
val detektAll by tasks.registering(Detekt::class) {
    group = "Quality"
    description = "Runs a detekt code analysis ruleset on the Wire Android codebase"
    parallel = true
    buildUponDefaultConfig = true

    val outputFile = "$buildDir/staticAnalysis/index.html"

    setSource(files(projectDir))
    config.setFrom("$rootDir/config/detekt/detekt.yml")

    include("**/*.kt")
    exclude("**/*.kts", "**/build/**", "/buildSrc")

    baseline.set(file("$rootDir/config/detekt/baseline.xml"))

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

tasks.withType(DetektCreateBaselineTask::class) {
    description = "Overrides current baseline."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(files(projectDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/baseline.xml"))

    include("**/*.kt")
    exclude("**/*.kts", "**/build/**", "/buildSrc")
}

tasks.register("staticCodeAnalysis") {
    description = "Analyses code within the Wire Android codebase"
    dependsOn(detektAll)
}

// Jacoco Configuration
val jacocoReport by tasks.registering(JacocoReport::class) {
    group = "Quality"
    description = "Reports code coverage on tests within the Wire Android codebase"
    val buildVariant = "devDebug" // It's not necessary to run unit tests on every variant so we default to "devDebug"
    dependsOn("test${buildVariant.capitalize()}UnitTest")

    val outputDir = "$buildDir/jacoco/html"

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(file(outputDir))
    }

    classDirectories.setFrom(
        fileTree(project.buildDir) {
            include(
                "**/classes/**/main/**", // This probably can be removed
                "**/tmp/kotlin-classes/$buildVariant/**"
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
                "**/mock/**",
                "**/*Screen*", // These are composable classes
                "**/*Kt*", // These are "usually" kotlin generated classes
                "**/theme/**/*.*", // Ignores jetpack compose theme related code
                "**/common/**/*.*", // Ignores jetpack compose common components related code
                "**/navigation/**/*.*" // Ignores jetpack navigation related code
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
