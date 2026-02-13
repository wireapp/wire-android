import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
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
abstract class WriteKeyValuesToFileTask : DefaultTask() {

    /**
     * The JSON file where the [keyValues] will be written
     */
    @get:OutputFile
    abstract val outputJsonFile: Property<File>

    /**
     * Map of key-value pairs that will be written to the [outputJsonFile].
     */
    @get:Input
    abstract val keyValues: MapProperty<String, String>

    init {
        group = "build"
        description = "Write a set of key-value pairs to a desired file"
    }

    @TaskAction
    fun processGitBuildIdentifier() {
        val outFile = outputJsonFile.get()
        require(!outFile.isDirectory) {
            "The specified output must be a regular file, not a directory: ${outFile.absolutePath}"
        }
        runCatching {
            logger.debug("\uD83D\uDD27 Writing key-values to ${outFile.absolutePath}.")
            keyValues.get().toJsonString().also { writeToFile(it) }
        }.onFailure {
            logger.error("\uD83D\uDD27 Failed to write key-values to file: ${it.stackTraceToString()}")
            writeToFile("{}")
        }
    }

    /**
     * Write the given [text] to the [outputJsonFile].
     */
    private fun writeToFile(text: String) {
        FileOutputStream(outputJsonFile.get()).use {
            it.write(text.toByteArray())
        }
        logger.debug("\u2705 Successfully wrote '$text' to $outputJsonFile.")
    }
}
