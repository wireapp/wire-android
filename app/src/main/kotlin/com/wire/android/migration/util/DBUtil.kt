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

package com.wire.android.migration.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

internal fun Context.openDatabaseIfExists(dbPath: String): SQLiteDatabase? = if (getDatabasePath(dbPath).exists()) {
    openOrCreateDatabase(dbPath, MODE_PRIVATE, null)
} else {
    null
}

internal fun Int.orNullIfNegative(): Int? = if (this < 0) null else this

internal fun Cursor.getStringOrNull(index: Int) = if (isNull(index)) null else getString(index)
internal fun Cursor.getBlobOrNull(index: Int) = if (isNull(index)) null else getBlob(index)
