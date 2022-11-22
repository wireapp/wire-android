package com.wire.android.migration.userDatabase

import android.database.Cursor
import com.wire.android.appLogger
import com.wire.android.migration.util.getStringOrNull
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

class ScalaUserDAO(private val db: ScalaUserDatabase) {

    fun allUsers(): List<ScalaUserData> {
        val cursor = db.rawQuery("SELECT * from $TABLE_NAME", arrayOf())
        return getUsersFromCursor(cursor)
    }

    fun users(userIds: List<String>): List<ScalaUserData> {
        val userIdsSelectionArg = userIds.joinToString(",")
        val cursor = db.rawQuery("SELECT * from $TABLE_NAME WHERE $COLUMN_ID IN (?)", arrayOf(userIdsSelectionArg))
        return getUsersFromCursor(cursor)
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
