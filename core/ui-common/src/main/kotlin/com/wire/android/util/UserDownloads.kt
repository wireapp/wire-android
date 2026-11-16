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
import java.security.MessageDigest

/**
 * The folder of one user's Cells downloads under the downloads root, `<root>/<domain>/<value>/cells`. The `cells`
 * subfolder keeps the files apart from Kalium's own `<domain>/<value>` asset folder, which shares the internal root.
 * Characters other than ASCII letters, digits, dot, dash and underscore become an underscore, and an empty part or
 * a dot segment becomes `_`. A part that changed gets `-` and the first 12 hex characters of the SHA-256 of the
 * original part, so different ids never share a folder and the result is always three levels inside [this].
 */
fun File.userDownloadsDir(userId: UserId): File =
    File(File(File(this, userId.domain.asFolderName()), userId.value.asFolderName()), CELLS_FOLDER)

/**
 * Deletes the Cells downloads of one user from app-specific external storage and from the internal files
 * directory, which downloads use when external storage is not available. Other users' files stay. The user and
 * domain folders are removed only when they are empty, so Kalium's files in them stay.
 */
fun Context.deleteUserDownloads(userId: UserId) {
    listOfNotNull(getExternalFilesDir(null), filesDir).forEach { root ->
        val cellsDir = root.userDownloadsDir(userId)
        cellsDir.deleteRecursively()
        cellsDir.parentFile?.delete()
        cellsDir.parentFile?.parentFile?.delete()
    }
}

private const val CELLS_FOLDER = "cells"
private const val HASH_LENGTH = 12

private fun String.asFolderName(): String {
    val sanitized = map { if (it.isSafeFolderCharacter()) it else '_' }
        .joinToString("")
        .takeUnless { it.isEmpty() || it == "." || it == ".." } ?: "_"
    return if (sanitized == this) this else "$sanitized-${sha256Prefix()}"
}

private fun String.sha256Prefix(): String =
    MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
        .take(HASH_LENGTH)

private fun Char.isSafeFolderCharacter(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this in ".-_"
