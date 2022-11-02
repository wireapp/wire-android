package com.wire.android.migration.userDatabase

import android.database.Cursor
import com.wire.android.appLogger
import com.wire.android.migration.util.getStringOrNull
import com.wire.kalium.logic.data.conversation.Conversation
import java.sql.SQLException

data class ScalaMessage(
    val conversationId: String,
    val protoBlob: ByteArray
)

class ScalaMessageDAO(private val db: ScalaUserDatabase) {

    fun messages(conversations: List<Conversation>): List<ScalaMessage> {
        val accumulator = mutableListOf<ScalaMessage>()
        var cursor: Cursor = db.rawQuery("SELECT 1", null)
        conversations.forEach { conversation ->
            cursor = db.rawQuery("SELECT * from $TABLE_NAME WHERE $COLUMN_CONVERSATION_ID = ?", arrayOf(conversation.id.value))
            try {
                val conversationIdIndex = cursor.getColumnIndex(COLUMN_CONVERSATION_ID)
                val protoBlobIndex = cursor.getColumnIndex(COLUMN_PROTO_BLOB)
                do {
                    val conversationId = cursor.getStringOrNull(conversationIdIndex).orEmpty()
                    val protoBlob = cursor.getBlob(protoBlobIndex)
                    accumulator += ScalaMessage(conversationId, protoBlob)
                } while (cursor.moveToNext())
            } catch (exception: SQLException) {
                appLogger.e("Error while querying old messages for conversationId ${conversation.id}: $exception")
            }
        }
        cursor.close()
        return accumulator
    }

    companion object {
        const val TABLE_NAME = "Messages"
        const val COLUMN_CONVERSATION_ID = "conv_id"
        const val COLUMN_PROTO_BLOB = "protos"
    }
}
