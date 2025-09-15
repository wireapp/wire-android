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

import java.io.File
import javax.inject.Inject

class FileNameResolver @Inject constructor() {
    /**
     * Generates a unique file name in the specified directory by appending a number in parentheses
     * if a file with the same name already exists.
     *
     * For example, if "document.txt" already exists, it will return "document(1).txt", and if that
     * also exists, it will return "document(2).txt", and so on.
     *
     * @param directory The directory to check for existing files.
     * @param originalFileName The original file name to make unique.
     * @return A File object with a unique name in the specified directory.
     */
    fun getUniqueFile(directory: File, originalFileName: String): File {
        val dotIndex = originalFileName.lastIndexOf('.')
        val baseName = if (dotIndex != -1) originalFileName.substring(0, dotIndex) else originalFileName
        val extension = if (dotIndex != -1) originalFileName.substring(dotIndex) else ""

        var file = File(directory, originalFileName)
        var index = 1

        while (file.exists()) {
            val newName = "$baseName($index)$extension"
            file = File(directory, newName)
            index++
        }
        return file
    }
}
