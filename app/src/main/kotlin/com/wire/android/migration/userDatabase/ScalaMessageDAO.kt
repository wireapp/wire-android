package com.wire.android.migration.userDatabase

import androidx.core.database.getLongOrNull
import com.wire.android.appLogger
import com.wire.android.migration.util.getStringOrNull
import java.sql.SQLException

data class ScalaMessageData(
    val id: String,
    val conversationId: String,
    val conversationRemoteId: String,
    val conversationDomain: String?,
    val time: Long,
    val editTime: Long?,
    val senderId: String,
    val senderClientId: String?,
    val content: String?,
    val proto: ByteArray?
) {
    @Suppress("ComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScalaMessageData

        if (id != other.id) return false
        if (conversationId != other.conversationId) return false
        if (conversationRemoteId != other.conversationRemoteId) return false
        if (conversationDomain != other.conversationDomain) return false
        if (time != other.time) return false
        if (editTime != other.editTime) return false
        if (senderId != other.senderId) return false
        if (senderClientId != other.senderClientId) return false
        if (content != other.content) return false
        if (proto != null) {
            if (other.proto == null) return false
            if (!proto.contentEquals(other.proto)) return false
        } else if (other.proto != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + conversationId.hashCode()
        result = 31 * result + conversationRemoteId.hashCode()
        result = 31 * result + (conversationDomain?.hashCode() ?: 0)
        result = 31 * result + time.hashCode()
        result = 31 * result + (editTime?.hashCode() ?: 0)
        result = 31 * result + senderId.hashCode()
        result = 31 * result + (senderClientId?.hashCode() ?: 0)
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + (proto?.contentHashCode() ?: 0)
        return result
    }
}

class ScalaMessageDAO(private val db: ScalaUserDatabase) {

    fun messages(scalaConversations: List<ScalaConversationData>): List<ScalaMessageData> {
        val accumulator = mutableListOf<ScalaMessageData>()
        scalaConversations.forEach { scalaConversation ->
            accumulator += messagesFromConversation(scalaConversation)
        }
        return accumulator
    }

    private fun messagesFromConversation(scalaConversation: ScalaConversationData): List<ScalaMessageData> {
        val cursor = db.rawQuery("SELECT * from $TABLE_NAME WHERE $COLUMN_CONVERSATION_ID = ?", arrayOf(scalaConversation.id))
        return try {
            if (!cursor.moveToFirst()) {
                emptyList()
            } else {
                val idIndex = cursor.getColumnIndex(COLUMN_ID)
                val timeIndex = cursor.getColumnIndex(COLUMN_TIME)
                val editTimeIndex = cursor.getColumnIndex(COLUMN_EDIT_TIME)
                val userIdIndex = cursor.getColumnIndex(COLUMN_USER_ID)
                val clientIdIndex = cursor.getColumnIndex(COLUMN_CLIENT_ID)
                val contentIndex = cursor.getColumnIndex(COLUMN_CONTENT)
                val protoIndex = cursor.getColumnIndex(COLUMN_PROTO_BLOB)
                val accumulator = mutableListOf<ScalaMessageData>()
                do {
                    accumulator += ScalaMessageData(
                        id = cursor.getStringOrNull(idIndex).orEmpty(),
                        conversationId = scalaConversation.id,
                        conversationRemoteId = scalaConversation.remoteId,
                        conversationDomain = scalaConversation.domain,
                        time = cursor.getLong(timeIndex),
                        editTime = cursor.getLongOrNull(editTimeIndex),
                        senderId = cursor.getStringOrNull(userIdIndex).orEmpty(),
                        senderClientId = cursor.getStringOrNull(clientIdIndex),
                        content = cursor.getStringOrNull(contentIndex),
                        proto = cursor.getBlob(protoIndex)
                    )
                } while (cursor.moveToNext())
                accumulator
            }
        } catch (exception: SQLException) {
            appLogger.e("Error while querying old messages $exception")
            emptyList()
        } finally {
            cursor.close()
        }
    }

    companion object {
        const val TABLE_NAME = "Messages"
        const val COLUMN_ID = "_id"
        const val COLUMN_CONVERSATION_ID = "conv_id"
        const val COLUMN_CLIENT_ID = "client_id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_TIME = "time"
        const val COLUMN_EDIT_TIME = "edit_time"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_PROTO_BLOB = "protos"
    }
}
