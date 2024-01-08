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

import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import com.wire.android.appLogger
import com.wire.android.migration.util.getBlobOrNull
import com.wire.android.migration.util.getStringOrNull
import com.wire.android.migration.util.orNullIfNegative
import kotlinx.coroutines.withContext
import java.sql.SQLException
import kotlin.coroutines.CoroutineContext

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
        if (assetName != other.assetName) return false
        if (assetSize != other.assetSize) return false

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
        result = 31 * result + (assetName?.hashCode() ?: 0)
        result = 31 * result + (assetSize ?: 0)
        return result
    }
}

class ScalaMessageDAO(
    private val db: ScalaUserDatabase,
    private val queryContext: CoroutineContext
) {

    suspend fun messages(scalaConversations: List<ScalaConversationData>): List<ScalaMessageData> =
        withContext(queryContext) {
            val accumulator = mutableListOf<ScalaMessageData>()
            scalaConversations.forEach { scalaConversation ->
                accumulator += messagesFromConversation(scalaConversation)
            }
            accumulator
        }

    private fun messagesFromConversation(scalaConversation: ScalaConversationData): List<ScalaMessageData> {
        val cursor = db.rawQuery(
            "SELECT *" + // Assets are required, otherwise we get exception "requesting column name with table name".
                    "FROM $MESSAGES_TABLE_NAME AS m " +
                    "LEFT JOIN $ASSETS_TABLE_NAME ON m.$COLUMN_ASSET_ID = $ASSETS_TABLE_NAME.$COLUMN_ID " +
                    "WHERE m.$COLUMN_CONVERSATION_ID = ?" +
                    "ORDER BY m.$COLUMN_TIME ASC ", arrayOf(scalaConversation.id)
        )
        return try {
            if (!cursor.moveToFirst()) {
                emptyList()
            } else {
                val timeIndex = cursor.getColumnIndex("$COLUMN_TIME")
                val editTimeIndex = cursor.getColumnIndex("$COLUMN_EDIT_TIME").orNullIfNegative()
                val userIdIndex = cursor.getColumnIndex("$COLUMN_USER_ID")
                val clientIdIndex = cursor.getColumnIndex("$COLUMN_CLIENT_ID").orNullIfNegative()
                val contentIndex = cursor.getColumnIndex("$COLUMN_CONTENT").orNullIfNegative()
                val protoIndex = cursor.getColumnIndex("$COLUMN_PROTO_BLOB").orNullIfNegative()
                val assetNameIndex = cursor.getColumnIndex("$COLUMN_NAME").orNullIfNegative()
                val assetSizeIndex = cursor.getColumnIndex("$COLUMN_SIZE").orNullIfNegative()
                val accumulator = mutableListOf<ScalaMessageData>()
                do {
                    accumulator += ScalaMessageData(
                        id = cursor.getString(0),
                        conversationId = scalaConversation.id,
                        conversationRemoteId = scalaConversation.remoteId,
                        conversationDomain = scalaConversation.domain,
                        time = cursor.getLong(timeIndex),
                        editTime = editTimeIndex?.let { cursor.getLongOrNull(it) },
                        senderId = cursor.getStringOrNull(userIdIndex).orEmpty(),
                        senderClientId = clientIdIndex?.let { cursor.getStringOrNull(it) },
                        content = contentIndex?.let { cursor.getStringOrNull(it) },
                        proto = protoIndex?.let { cursor.getBlobOrNull(it) },
                        assetName = assetNameIndex?.let { cursor.getStringOrNull(it) },
                        assetSize = assetSizeIndex?.let { cursor.getIntOrNull(it) }
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
