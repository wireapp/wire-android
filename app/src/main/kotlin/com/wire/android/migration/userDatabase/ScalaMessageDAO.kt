package com.wire.android.migration.userDatabase

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

        val cursor = db.rawQuery("SELECT * from $TABLE_NAME", null)
        return try {
            val conversationIdIndex = cursor.getColumnIndex(COLUMN_CONVERSATION_ID)
            val protoBlobIndex = cursor.getColumnIndex(COLUMN_PROTO_BLOB)
            val accumulator = mutableListOf<ScalaMessage>()
            do {
                val conversationId = cursor.getStringOrNull(conversationIdIndex).orEmpty()
                val protoBlob = cursor.getBlob(protoBlobIndex)
                accumulator += ScalaMessage(conversationId, protoBlob)
            } while (cursor.moveToNext())
            accumulator
        } catch (exception: SQLException) {
            appLogger.e("Error while querying old messages $exception")
            emptyList()
        } finally {
            cursor.close()
        }
    }

    companion object {
        const val TABLE_NAME = "Messages"
        const val COLUMN_CONVERSATION_ID = "conv_id"
        const val COLUMN_PROTO_BLOB = "protos"
    }
}
