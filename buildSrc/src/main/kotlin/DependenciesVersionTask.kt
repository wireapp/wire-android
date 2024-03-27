import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream

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
open class DependenciesVersionTask : DefaultTask() {

    private val VERSION_FILE = "app/src/main/assets/dependencies_version.json"

    // map of toml file and list of dependencies to extract version
    private val dependencies = mapOf(
        "kalium/gradle/libs.versions.toml" to listOf(
            "avs",
            "core-crypto"
        )
    )

    init {
        group = "release"
        description = "Get dependencies version and write it to the application, if possible."
    }

    @TaskAction
    fun processGitBuildIdentifier() {
        runCatching {
            println("\uD83D\uDD27 Running dependencies version parser to build.")
            mutableMapOf<String, String?>().apply {
                for ((tomlFile, dependencies) in dependencies) {
                    val toml = File(tomlFile).readText()
                    val tables = parseToml(toml)
                    for (dependency in dependencies) {
                        val version = tables["versions"]?.get(dependency)
                        println("\uD83D\uDD27 $dependency version: $version")
                        put(dependency, version)
                    }
                }
            }.toJsonString()
                .also { writeToFile(it) }
        }.onFailure {
            println("\uD83D\uDD27 Failed to extract dependencies version: ${it.stackTraceToString()}")
            writeToFile("{}")
        }
    }

    fun parseToml(tomlContent: String): Map<String, Map<String, String>> {
        val table = mutableMapOf<String, MutableMap<String, String>>()
        var currentTable = ""

        // Regular expression to match table headers and key-value pairs
        val regex = Regex("""\[(.*?)\]|\s*([^\s=]+)\s*=\s*(".*?"|[^\r\n#]+)""")

        // Iterate over lines of the TOML content
        tomlContent.lines().forEach { line ->
            val matchResult = regex.find(line)

            // If it's a table header
            if (line.startsWith("[")) {
                currentTable = matchResult?.groups?.get(1)?.value ?: ""
                table[currentTable] = mutableMapOf()
            }
            // If it's a key-value pair
            else if (matchResult != null) {
                val key = matchResult.groups[2]?.value?.trim('"')
                val value = matchResult.groups[3]?.value?.trim('"')

                if (!key.isNullOrBlank() && !value.isNullOrBlank()) {
                    table[currentTable]?.put(key, value)
                }
            }
        }
        return table
    }

    /**
     * Write the given [text] to the [VERSION_FILE].
     */
    private fun writeToFile(text: String) {
        FileOutputStream(File(project.rootDir, VERSION_FILE)).use {
            it.write(text.toByteArray())
        }
        println("\u2705 Successfully wrote $text to file.")
    }
}
