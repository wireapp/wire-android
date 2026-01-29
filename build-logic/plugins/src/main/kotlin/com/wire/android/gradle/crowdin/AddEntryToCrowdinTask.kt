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
package com.wire.android.gradle.crowdin

import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class AddEntryToCrowdinTask : DefaultTask() {

    @get:Input
    abstract var resDirPath: String

    private val rootProject: Project
        get() = project.rootProject

    @OutputFile
    val outputFile: File = project.rootDir.resolve(CROWDIN_FILE_NAME)

    @OptIn(ExperimentalStdlibApi::class)
    @TaskAction
    fun taskAction() {
        val expectedStringFileEntry = File(resDirPath)
            .resolve(DEFAULT_SOURCE_STRING_FILE_PATH)
            .takeIf { it.exists() }
            ?.relativeTo(rootProject.rootDir)
            ?.invariantSeparatorsPath
            ?.let { "/$it" } ?: return
        val crowdinFile = outputFile
        val crowdinFileContent = crowdinFile.readText()
        val filesEntry = crowdinFileContent.substringAfter(FILES_ENTRY_DELIMITER)
        val restOfFile = crowdinFileContent.substring(0..<crowdinFileContent.length - filesEntry.length)
        val json = Json { prettyPrint = true }
        val entries = json.decodeFromString<List<CrowdinFileEntry>>(filesEntry).toMutableList()
        val isNewEntryNeeded = expectedStringFileEntry !in entries.map { it.source }
        if (!isNewEntryNeeded) return

        val newEntry = CrowdinFileEntry(
            source = expectedStringFileEntry,
            translation = expectedStringFileEntry.replace(DEFAULT_SOURCE_STRING_FILE_PATH, TRANSLATION_RES_STRING_FILE_PATH)
        )
        entries.add(newEntry)
        println("Adding new entry to Crowdin file: ${json.encodeToString(CrowdinFileEntry.serializer(), newEntry)}")

        val newFilesEntryContent = json.encodeToString(entries.sortedBy { it.source })
        crowdinFile.writeText(restOfFile + newFilesEntryContent)
    }

    private companion object {
        const val DEFAULT_SOURCE_STRING_FILE_PATH = "values/strings.xml"
        const val TRANSLATION_RES_STRING_FILE_PATH = "values-%two_letters_code%/%original_file_name%"
        const val CROWDIN_FILE_NAME = "crowdin.yml"
        const val FILES_ENTRY_DELIMITER = "files: "
    }
}
