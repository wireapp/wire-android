package com.wire.android.migration.userDatabase

import com.wire.android.appLogger
import com.wire.android.migration.util.getStringOrNull
import java.sql.SQLException

data class ScalaUserData(
    val id: String,
    val domain: String?,
    val teamId: String?
)

class ScalaUserDAO(private val db: ScalaUserDatabase) {

    fun users(userIds: List<String>): List<ScalaUserData> {
        val userIdsSelectionArg = userIds.joinToString(",")
        val cursor = db.rawQuery("SELECT * from $TABLE_NAME WHERE $COLUMN_ID IN (?)", arrayOf(userIdsSelectionArg))
        return try {
            if (!cursor.moveToFirst()) {
                emptyList()
            } else {
                val accumulator = mutableListOf<ScalaUserData>()
                val idIndex = cursor.getColumnIndex(COLUMN_ID)
                val domainIndex = cursor.getColumnIndex(COLUMN_DOMAIN)
                val teamIdIndex = cursor.getColumnIndex(COLUMN_TEAM_ID)
                do {
                    accumulator += ScalaUserData(
                        id = cursor.getStringOrNull(idIndex).orEmpty(),
                        domain = cursor.getStringOrNull(domainIndex),
                        teamId = cursor.getStringOrNull(teamIdIndex)
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
        const val COLUMN_TEAM_ID = "team_id"
    }
}
