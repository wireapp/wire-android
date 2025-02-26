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

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import scripts.Variants_gradle.Default
import toJsonStringList
import java.security.MessageDigest
import kotlin.math.abs

tasks.withType(Test::class) {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

val shardsFilePath = "${project.rootDir}/shards.json"
val shards = listOf(0, 1, 2, 3)

/**
 * Helper function to generate a SHA-256 hash of a file name
 */
fun getFileHash(fileName: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(fileName.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}

/**
 * Task to generate sharded test assignments for parallel test execution
 */
tasks.register("generateShardsForTests") {
    // Ensure tests are compiled before generating the shard
    dependsOn(tasks.named("compile${Default.BUILD_VARIANT}UnitTestKotlin"))
    doLast {
        val testClasses = mutableListOf<String>()
        // Search for compiled Java unit test classes
        fileTree("build/classes/java/test") {
            include("**/*Test.class") // For Java unit test files
        }.forEach { file ->
            testClasses.add(file.path)
        }
        // Search for compiled Kotlin unit test classes
        fileTree("build/intermediates/classes/${Default.BUILD_VARIANT.decapitalize()}UnitTest") {
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

        // Map to store tests assigned to each shard
        val numShards = shards.size // Number of shards (adjust this as needed)
        val shards = mutableMapOf<Int, MutableList<String>>()
        // Assign each test class to a shard based on the hash of the file name
        testClasses.forEach { file ->
            val hash = getFileHash(file)
            println("Sharding test class: $file with hash: $hash")
            val shardIndex = abs(hash.hashCode() % numShards)
            println("Sharding $hash to shard $shardIndex")
            // add the test classes to the shard with the name ready for Test filters.
            shards.put(shardIndex, shards.getOrDefault(shardIndex, mutableListOf()).apply { add("**/*" + file.substringAfterLast("/")) })
        }
        val shardFile = file(shardsFilePath)
        shardFile.writeText(
            shards.toJsonStringList()
        )
    }
}

/**
 * Create N-[shards] test tasks for parallel test execution
 */
project.afterEvaluate {
    shards.forEach { index ->
        tasks.register("unitTestShard$index", DefaultTask::class) {
            group = "verification"
            description = "Run tests for Shard $index"

            val file = file("${project.rootDir}/shards.json")
            val json = groovy.json.JsonSlurper().parseText(file.readText()) as Map<String, List<String>>
            val classes: List<String> = json[index.toString()] ?: emptyList()
            val task: Test = tasks.getByName("test${Default.BUILD_VARIANT}UnitTest", Test::class).apply {
                include(classes) // include only the test classes assigned to this shard
            }

            finalizedBy(task)
        }
    }
}
