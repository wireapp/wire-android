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

package com.wire.android.util

import androidx.annotation.VisibleForTesting
import com.wire.kalium.logic.util.buildFileName
import com.wire.kalium.logic.util.splitFileExtensionAndCopyCounter
import java.io.File

@VisibleForTesting
fun findFirstUniqueName(dir: File, desiredName: String): String {
    var currentName: String = desiredName.sanitizeFilename()
    while (File(dir, currentName).exists()) {
        val (nameWithoutCopyCounter, copyCounter, extension) = currentName.splitFileExtensionAndCopyCounter()
        currentName = buildFileName(nameWithoutCopyCounter, extension, copyCounter + 1).sanitizeFilename()
    }
    return currentName
}

/**
 * Removes disallowed characters and returns valid filename.
 *
 * Uses the same cases as in `isValidFatFilenameChar` and `isValidExtFilenameChar` from [android.os.FileUtils].
 */
@VisibleForTesting
fun String.sanitizeFilename(): String = replace(Regex("[\u0000-\u001f\u007f\"*/:<>?\\\\|]"), "_")
