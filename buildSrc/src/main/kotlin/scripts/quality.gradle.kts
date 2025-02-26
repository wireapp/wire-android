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
import java.security.MessageDigest
import kotlin.math.abs

plugins {
    id("com.android.application") apply false
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    val detektVersion = findVersion("detekt").requiredVersion
    detekt("io.gitlab.arturbosch.detekt:detekt-cli:$detektVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:$detektVersion")
    detektPlugins("com.wire:detekt-rules:1.0.0-1.23.6") {
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

    setSource(files(rootDir))
    config.setFrom("$rootDir/config/detekt/detekt.yml")

    include("**/*.kt")
    exclude("**/*.kts", "**/build/**", "/buildSrc", "/kalium", "/template")

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

tasks.register("testCoverage") {
    group = "Quality"
    description = "Reports code coverage on tests within the Wire Android codebase."
    dependsOn("koverXmlReport")
}

// Function to hash a string (in this case, the file name)
fun getFileHash(fileName: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(fileName.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}
// Gradle task to generate sharded test lists
tasks.register("generateShardedTests") {
    // dependsOn(tasks.named("compileDevDebugUnitTestKotlin")) // Ensure tests are compiled before generating the shard
    doLast {
        val testClasses = mutableListOf<String>()
        // Search for compiled Java unit test classes
        fileTree("build/classes/java/test") {
            include("**/*Test.class") // For Java unit test files
        }.forEach { file ->
            testClasses.add(file.path)
        }
        // Search for compiled Kotlin unit test classes
        fileTree("build/intermediates/classes/devDebugUnitTest") {
            include("**/*Test.class") // For Kotlin unit test files
        }.forEach { file ->
            testClasses.add(file.path)
        }
        // Search for compiled Java instrumentation test classes
        fileTree("build/intermediates/classes/debug") {
            include("**/*Test.class") // For Java instrumentation test files
        }.forEach { file ->
            testClasses.add(file.path)
        }
        // Debugging: Check if any test classes are found
        if (testClasses.isEmpty()) {
            println("No test classes found!")
        } else {
            println("Test classes found: ${testClasses.size}")
        }
        // Map to store tests assigned to each shard
        val numShards = shards.size // Number of shards (adjust this as needed)
        val shards = mutableMapOf<Int, MutableList<String>>()
        // Assign each test class to a shard based on the hash of the file name
        testClasses.forEach { file ->
            val hash = getFileHash(file)
            println("Sharding test class: $file with hash: $hash")
            val shardIndex = abs(hash.hashCode() % numShards)
            println("Sharding $hash to shard $shardIndex")
            shards.put(shardIndex, shards.getOrDefault(shardIndex, mutableListOf()).apply { add(file) })
        }
        // Output the shard assignments to the console (could also save to a file or pass to GitHub Actions)
        shards.forEach { (shard, tests) ->
            println("Shard ${shard + 1}: ${tests.joinToString("\n")}")
        }
        // Optionally save the shard assignments to a file (e.g., for later use in GitHub Actions)
        val shardFile = file("${project.rootDir}/shards.txt")
        shardFile.writeText(
            shards.map { (shard, tests) ->
                "Shard ${shard + 1}:\n${tests.joinToString("\n")}"
            }.joinToString("\n\n")
        )
    }
}

val shardGroups = mutableMapOf<Int, MutableList<String>>()
val shardsFilePath = "${project.rootDir}/shards.json"

val shards = listOf(1, 2, 3, 4)
project.afterEvaluate {
    shards.forEach { index ->
        tasks.register("testShard$index", Test::class) {
            group = "verification"
            description = "Run tests for Shard $index"

            val file = File(shardsFilePath)
            val json = groovy.json.JsonSlurper().parseText(file.readText()) as Map<String, List<String>>
            val classes: List<String> = json[index.toString()] ?: emptyList()

            useJUnitPlatform()

            classpath = sourceSets["test"].runtimeClasspath
            testClassesDirs = sourceSets["test"].output.classesDirs

            println("Running tests for shard $index: $classes")
            setIncludes(classes)
        }
    }
}

tasks.register<Test>("MyTests") {
    group = "MyCustomTasks"
    filter {
        includeTestsMatching("ir.mahozad.*Convert*")
    }
}

