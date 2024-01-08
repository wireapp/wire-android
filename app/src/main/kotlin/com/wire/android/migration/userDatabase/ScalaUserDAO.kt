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

package com.wire.android.migration.userDatabase

import android.database.Cursor
import com.wire.android.appLogger
import com.wire.android.migration.util.getStringOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.sql.SQLException

data class ScalaUserData(
    val id: String,
    val domain: String?,
    val teamId: String?,
    val name: String,
    val handle: String?,
    val email: String?,
    val phone: String?,
    val accentId: Int,
    val connection: String,
    val pictureAssetId: String?,
    val availability: Int,
    val deleted: Boolean,
    val serviceProviderId: String?,
    val serviceIntegrationId: String?
)

class ScalaUserDAO(
    private val db: ScalaUserDatabase,
    private val coroutineDispatcher: CoroutineDispatcher
) {

    suspend fun allUsers(): List<ScalaUserData> = withContext(coroutineDispatcher) {
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", arrayOf())
        getUsersFromCursor(cursor)
    }

    suspend fun users(userIds: List<String>): List<ScalaUserData> = withContext(coroutineDispatcher) {
        val sqlQuery = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_ID IN (?)"
        val userIdsSelectionArg = userIds.joinToString(separator = "','", prefix = "'", postfix = "'")
        val cursor = db.rawQuery(sqlQuery.replace("?", userIdsSelectionArg), null)
        getUsersFromCursor(cursor)
    }

    private fun getUsersFromCursor(cursor: Cursor): List<ScalaUserData> {
        return try {
            if (!cursor.moveToFirst()) {
                emptyList()
            } else {
                val accumulator = mutableListOf<ScalaUserData>()
                val idIndex = cursor.getColumnIndex(COLUMN_ID)
                val domainIndex = cursor.getColumnIndex(COLUMN_DOMAIN)
                val teamIdIndex = cursor.getColumnIndex(COLUMN_TEAM_ID)
                val nameIndex = cursor.getColumnIndex(COLUMN_NAME)
                val handleIndex = cursor.getColumnIndex(COLUMN_HANDLE)
                val emailIndex = cursor.getColumnIndex(COLUMN_EMAIL)
                val phoneIndex = cursor.getColumnIndex(COLUMN_PHONE)
                val accentIndex = cursor.getColumnIndex(COLUMN_ACCENT)
                val connectionIndex = cursor.getColumnIndex(COLUMN_CONNECTION)
                val pictureIndex = cursor.getColumnIndex(COLUMN_PICTURE)
                val availabilityIndex = cursor.getColumnIndex(COLUMN_AVAILABILITY)
                val deletedIndex = cursor.getColumnIndex(COLUMN_DELETED)
                val providerIdIndex = cursor.getColumnIndex(COLUMN_PROVIDER_ID)
                val integrationIdIndex = cursor.getColumnIndex(COLUMN_INTEGRATION_ID)
                do {
                    accumulator += ScalaUserData(
                        id = cursor.getStringOrNull(idIndex).orEmpty(),
                        domain = cursor.getStringOrNull(domainIndex),
                        teamId = cursor.getStringOrNull(teamIdIndex),
                        name = cursor.getString(nameIndex),
                        handle = cursor.getStringOrNull(handleIndex),
                        email = cursor.getStringOrNull(emailIndex),
                        phone = cursor.getStringOrNull(phoneIndex),
                        accentId = cursor.getInt(accentIndex),
                        connection = cursor.getString(connectionIndex),
                        pictureAssetId = cursor.getStringOrNull(pictureIndex),
                        availability = cursor.getInt(availabilityIndex),
                        deleted = cursor.getInt(deletedIndex) == 1,
                        serviceProviderId = cursor.getStringOrNull(providerIdIndex),
                        serviceIntegrationId = cursor.getStringOrNull(integrationIdIndex)
                    )
                } while (cursor.moveToNext())
                accumulator
            }
        } catch (exception: SQLException) {
            appLogger.e("Error while querying old conversations $exception")
            emptyList()
        } finally {
            cursor.close()
        }
    }

    companion object {
        const val TABLE_NAME = "Users"
        const val COLUMN_ID = "_id"
        const val COLUMN_DOMAIN = "domain"
        const val COLUMN_TEAM_ID = "teamId"
        const val COLUMN_NAME = "name"
        const val COLUMN_HANDLE = "handle"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_ACCENT = "accent"
        const val COLUMN_CONNECTION = "connection"
        const val COLUMN_PICTURE = "picture"
        const val COLUMN_AVAILABILITY = "availability"
        const val COLUMN_DELETED = "deleted"
        const val COLUMN_PROVIDER_ID = "provider_id"
        const val COLUMN_INTEGRATION_ID = "integration_id"
    }
}
