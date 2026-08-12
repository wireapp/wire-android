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
package com.wire.android.ui.debug.securityproviders

import android.content.Context
import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.kalium.logic.data.user.UserId
import java.io.File

class AppPathsProvider(
    private val context: Context,
    private val currentAccount: UserId,
) {
    operator fun invoke(): List<AppPathEntry> = with(context) {
        val accountSuffix = "${currentAccount.domain}/${currentAccount.value}"
        listOf(
            AppPathEntry(R.string.debug_settings_app_path_assets, "$filesDir/$accountSuffix"),
            AppPathEntry(R.string.debug_settings_app_path_cache, "$cacheDir/$accountSuffix"),
            AppPathEntry(R.string.debug_settings_app_path_files_dir, filesDir.absolutePath),
            AppPathEntry(R.string.debug_settings_app_path_cache_dir, cacheDir.absolutePath),
            AppPathEntry(R.string.debug_settings_app_path_databases_dir, getDatabasePath(DATABASE_NAME_PROBE).parent.orEmpty()),
            AppPathEntry(R.string.debug_settings_app_path_no_backup_dir, noBackupFilesDir.absolutePath),
            AppPathEntry(R.string.debug_settings_app_path_external_files_dir, getExternalFilesDir(null)?.absolutePath.orEmpty()),
        )
    }

    fun userDatabaseSecurityStatus(): UserDatabaseSecurityStatus {
        val databaseFile = context.getDatabasePath(
            currentAccount.databaseFileName()
        )
        val databasePath = databaseFile.absolutePath
        return UserDatabaseSecurityStatus(
            path = databasePath,
            header = databaseFile.sqliteHeaderStatus(),
            isInInternalDataDirectory = databasePath.isWithin(context.applicationInfo.dataDir),
        )
    }

    private companion object {
        /** Only used to resolve the databases directory, the file itself never has to exist. */
        const val DATABASE_NAME_PROBE = "probe"
    }
}

private fun UserId.databaseFileName(): String =
    "user-db-$value-$domain".filter { it.isLetterOrDigit() || it == '-' }

internal fun File.sqliteHeaderStatus(): SqliteHeaderStatus = when {
    !exists() -> SqliteHeaderStatus.NotCreated
    !isFile -> SqliteHeaderStatus.Unavailable
    else -> runCatching {
        inputStream().use { input ->
            val header = ByteArray(SQLITE_HEADER.size)
            val bytesRead = input.read(header)
            if (bytesRead == SQLITE_HEADER.size) {
                if (header.contentEquals(SQLITE_HEADER)) {
                    SqliteHeaderStatus.PlainSqlite
                } else {
                    SqliteHeaderStatus.NotPlainSqlite(header.toHexString())
                }
            } else {
                SqliteHeaderStatus.Unavailable
            }
        }
    }.getOrElse { SqliteHeaderStatus.Unavailable }
}

private fun String.isWithin(directory: String): Boolean =
    runCatching {
        val canonicalDirectory = File(directory).canonicalFile
        File(this).canonicalFile.toPath().startsWith(canonicalDirectory.toPath())
    }.getOrDefault(false)

private val SQLITE_HEADER = "SQLite format 3\u0000".encodeToByteArray()

data class UserDatabaseSecurityStatus(
    val path: String,
    val header: SqliteHeaderStatus,
    val isInInternalDataDirectory: Boolean,
)

sealed interface SqliteHeaderStatus {
    val value: String

    data object PlainSqlite : SqliteHeaderStatus {
        override val value: String = "SQLite format 3\\u0000"
    }

    data class NotPlainSqlite(override val value: String) : SqliteHeaderStatus

    data object NotCreated : SqliteHeaderStatus {
        override val value: String = "Not created"
    }

    data object Unavailable : SqliteHeaderStatus {
        override val value: String = "Unavailable"
    }
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

data class AppPathEntry(
    @StringRes val labelRes: Int,
    val path: String,
)
