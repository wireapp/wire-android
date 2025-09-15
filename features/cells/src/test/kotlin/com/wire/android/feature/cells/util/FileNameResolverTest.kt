/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

class FileNameResolverTest {

    @Test
    fun given_file_does_not_existWhen_getUniqueFile_calledThen_returns_original_file_name() {
        // Given
        val tempDir = createTempDir()
        val fileName = "document.txt"

        // When
        val result = FileNameResolver().getUniqueFile(tempDir, fileName)

        // Then
        Assertions.assertEquals("document.txt", result.name)
        assertFalse(result.exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun given_file_existsWhen_getUniqueFile_calledThen_returns_file_name_with_1_suffix() {
        // Given
        val tempDir = createTempDir()
        File(tempDir, "image.png").createNewFile()

        // When
        val result = FileNameResolver().getUniqueFile(tempDir, "image.png")

        // Then
        Assertions.assertEquals("image(1).png", result.name)
        assertFalse(result.exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun given_multiple_conflicting_files_existWhen_getUniqueFile_calledThen_returns_file_name_with_next_available_index() {
        // Given
        val tempDir = createTempDir()
        File(tempDir, "file.txt").createNewFile()
        File(tempDir, "file(1).txt").createNewFile()
        File(tempDir, "file(2).txt").createNewFile()

        // When
        val result = FileNameResolver().getUniqueFile(tempDir, "file.txt")

        // Then
        Assertions.assertEquals("file(3).txt", result.name)
        assertFalse(result.exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun given_file_without_extension_existsWhen_getUniqueFile_calledThen_returns_file_name_with_1_suffix() {
        // Given
        val tempDir = createTempDir()
        File(tempDir, "LICENSE").createNewFile()

        // When
        val result = FileNameResolver().getUniqueFile(tempDir, "LICENSE")

        // Then
        Assertions.assertEquals("LICENSE(1)", result.name)
        assertFalse(result.exists())

        tempDir.deleteRecursively()
    }
}
