package com.wire.android.migration.userDatabase

import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import com.wire.android.appLogger
import com.wire.android.migration.util.getStringOrNull
import com.wire.android.migration.util.orNullIfNegative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.SQLException
import kotlin.coroutines.CoroutineContext

data class ScalaConversationData(
    val id: String,
    val remoteId: String,
    val domain: String?,
    val name: String?,
    val type: Int,
    val teamId: String?,
    val mutedStatus: Int,
    val access: String,
    val creatorId: String,
    val receiptMode: Int?,
    val orderTime: Long?,
    val lastReadTime: Long?
)

class ScalaConversationDAO(
    private val db: ScalaUserDatabase,
    private val quiresContext: CoroutineContext = Dispatchers.IO
) {

    suspend fun conversations(): List<ScalaConversationData> = withContext(quiresContext) {
        val cursor = db.rawQuery("SELECT * from $TABLE_NAME", null)

        try {
            val domainIndex = cursor.getColumnIndex(COLUMN_DOMAIN).orNullIfNegative()
            val idIndex = cursor.getColumnIndex(COLUMN_ID)
            val remoteIdIndex = cursor.getColumnIndex(COLUMN_REMOTE_ID)
            val nameIndex = cursor.getColumnIndex(COLUMN_NAME)
            val typeIndex = cursor.getColumnIndex(COLUMN_TYPE)
            val teamIndex = cursor.getColumnIndex(COLUMN_TEAM)
            val mutedStatusIndex = cursor.getColumnIndex(COLUMN_MUTED_STATUS)
            val accessIndex = cursor.getColumnIndex(COLUMN_ACCESS)
            val creatorIdIndex = cursor.getColumnIndex(COLUMN_CREATOR)
            val receiptModeIndex = cursor.getColumnIndex(COLUMN_RECEIPT_MODE).orNullIfNegative()
            val lastEventTimeIndex = cursor.getColumnIndex(COLUMN_LAST_EVENT_TIME).orNullIfNegative()
            val lastReadTimeIndex = cursor.getColumnIndex(COLUMN_LAST_READ).orNullIfNegative()

            if (!cursor.moveToFirst()) {
                return@withContext emptyList()
            }

            val accumulator = mutableListOf<ScalaConversationData>()
            do {
                val remoteId = cursor.getStringOrNull(remoteIdIndex)
                if (remoteId.isNullOrBlank()) continue // jump to the next conversation if the remote id is missing

                accumulator += ScalaConversationData(
                    id = cursor.getStringOrNull(idIndex).orEmpty(),
                    remoteId = remoteId,
                    domain = domainIndex?.let { cursor.getStringOrNull(it) },
                    name = cursor.getStringOrNull(nameIndex),
                    type = cursor.getInt(typeIndex),
                    teamId = cursor.getStringOrNull(teamIndex),
                    mutedStatus = cursor.getInt(mutedStatusIndex),
                    access = cursor.getStringOrNull(accessIndex).orEmpty(),
                    creatorId = cursor.getStringOrNull(creatorIdIndex).orEmpty(),
                    receiptMode = receiptModeIndex?.let { cursor.getIntOrNull(it) },
                    orderTime = lastEventTimeIndex?.let { cursor.getLongOrNull(it) },
                    lastReadTime = lastReadTimeIndex?.let { cursor.getLongOrNull(it) }
                )
            } while (cursor.moveToNext())
            accumulator
        } catch (exception: SQLException) {
            appLogger.e("Error while querying old conversations $exception")
            emptyList()
        } finally {
            cursor.close()
        }
    }

    companion object {
        const val TABLE_NAME = "Conversations"
        const val COLUMN_ID = "_id"
        const val COLUMN_REMOTE_ID = "remote_id"
        const val COLUMN_DOMAIN = "domain"
        const val COLUMN_NAME = "name"
        const val COLUMN_TYPE = "conv_type"
        const val COLUMN_TEAM = "team"
        const val COLUMN_MUTED_STATUS = "muted_status"
        const val COLUMN_ACCESS = "access"
        const val COLUMN_CREATOR = "creator"
        const val COLUMN_RECEIPT_MODE = "receipt_mode"
        const val COLUMN_LAST_EVENT_TIME = "last_event_time"
        const val COLUMN_LAST_READ = "last_read"
    }
}
