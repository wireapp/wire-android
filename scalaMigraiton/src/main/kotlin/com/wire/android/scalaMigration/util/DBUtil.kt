package com.wire.android.scalaMigration.util

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
