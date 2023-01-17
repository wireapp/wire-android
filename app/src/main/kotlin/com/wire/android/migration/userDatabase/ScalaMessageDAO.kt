package com.wire.android.migration.userDatabase

import androidx.core.database.getIntOrNull
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
    val proto: ByteArray?,
    val assetName: String?,
    val assetSize: Int?,
)

class ScalaMessageDAO(private val db: ScalaUserDatabase) {

    fun messages(scalaConversations: List<ScalaConversationData>): List<ScalaMessageData> {
        val accumulator = mutableListOf<ScalaMessageData>()
        scalaConversations.forEach { scalaConversation ->
            accumulator += messagesFromConversation(scalaConversation)
        }
        return accumulator
    }

    private fun messagesFromConversation(scalaConversation: ScalaConversationData): List<ScalaMessageData> {
        val cursor = db.rawQuery(
            "SELECT * from $MESSAGES_TABLE_NAME " +
                    "LEFT JOIN $ASSETS_TABLE_NAME ON $MESSAGES_TABLE_NAME.$COLUMN_ASSET_ID = $ASSETS_TABLE_NAME.$COLUMN_ID " +
                    "WHERE $COLUMN_CONVERSATION_ID = ?", arrayOf(scalaConversation.id)
        )
        return try {
            if (!cursor.moveToFirst()) {
                emptyList()
            } else {
                val idIndex = cursor.getColumnIndex("$MESSAGES_TABLE_NAME.$COLUMN_ID")
                val timeIndex = cursor.getColumnIndex("$MESSAGES_TABLE_NAME.$COLUMN_TIME")
                val editTimeIndex = cursor.getColumnIndex("$MESSAGES_TABLE_NAME.$COLUMN_EDIT_TIME")
                val userIdIndex = cursor.getColumnIndex("$MESSAGES_TABLE_NAME.$COLUMN_USER_ID")
                val clientIdIndex = cursor.getColumnIndex("$MESSAGES_TABLE_NAME.$COLUMN_CLIENT_ID")
                val contentIndex = cursor.getColumnIndex("$MESSAGES_TABLE_NAME.$COLUMN_CONTENT")
                val protoIndex = cursor.getColumnIndex("$MESSAGES_TABLE_NAME.$COLUMN_PROTO_BLOB")
                val assetNameIndex = cursor.getColumnIndex("$ASSETS_TABLE_NAME.$COLUMN_NAME")
                val assetSizeIndex = cursor.getColumnIndex("$ASSETS_TABLE_NAME.$COLUMN_SIZE")
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
                        proto = cursor.getBlob(protoIndex),
                        assetName = cursor.getStringOrNull(assetNameIndex),
                        assetSize = cursor.getIntOrNull(assetSizeIndex)
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
        const val MESSAGES_TABLE_NAME = "Messages"
        const val ASSETS_TABLE_NAME = "Assets2"
        const val COLUMN_ID = "_id"
        const val COLUMN_ASSET_ID = "asset_id"
        const val COLUMN_CONVERSATION_ID = "conv_id"
        const val COLUMN_CLIENT_ID = "client_id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_TIME = "time"
        const val COLUMN_EDIT_TIME = "edit_time"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_PROTO_BLOB = "protos"
        const val COLUMN_NAME = "name"
        const val COLUMN_SIZE = "size"
    }
}
