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

import android.content.Context
import com.wire.kalium.logic.data.user.UserId
import java.io.File

/**
 * The folder of one user's Cells downloads under the downloads root, `<root>/<domain>/<value>`.
 * Characters other than ASCII letters, digits, dot, dash and underscore become an underscore, and an empty
 * part or a dot segment becomes `_`, so the result is always two levels inside [this].
 */
fun File.userDownloadsDir(userId: UserId): File =
    File(File(this, userId.domain.asFolderName()), userId.value.asFolderName())

/**
 * Deletes the Cells downloads of one user from app-specific external storage and from the internal files
 * directory, which downloads use when external storage is not available. Other users' files stay.
 */
fun Context.deleteUserDownloads(userId: UserId) {
    listOfNotNull(getExternalFilesDir(null), filesDir).forEach { it.userDownloadsDir(userId).deleteRecursively() }
}

private fun String.asFolderName(): String =
    map { if (it.isSafeFolderCharacter()) it else '_' }
        .joinToString("")
        .takeUnless { it.isEmpty() || it == "." || it == ".." } ?: "_"

private fun Char.isSafeFolderCharacter(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this in ".-_"
