/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.migration.userDatabase

import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import com.wire.android.appLogger
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

    suspend fun messagesForConversation(scalaConversation: ScalaConversationData): List<ScalaMessageData> =
        withContext(queryContext) {
            messagesFromConversation(scalaConversation)
        }

    private fun messagesFromConversation(scalaConversation: ScalaConversationData): List<ScalaMessageData> {
        val cursor = db.rawQuery(
            "SELECT " + // Assets are required, otherwise we get exception "requesting column name with table name".
                    "$MESSAGES_TABLE_NAME.$COLUMN_ID AS $MESSAGE_ALIAS_PREFIX$COLUMN_ID, " +
                    "$MESSAGES_TABLE_NAME.$COLUMN_TIME AS $MESSAGE_ALIAS_PREFIX$COLUMN_TIME, " +
                    "$MESSAGES_TABLE_NAME.$COLUMN_EDIT_TIME AS $MESSAGE_ALIAS_PREFIX$COLUMN_EDIT_TIME, " +
                    "$MESSAGES_TABLE_NAME.$COLUMN_USER_ID AS $MESSAGE_ALIAS_PREFIX$COLUMN_USER_ID, " +
                    "$MESSAGES_TABLE_NAME.$COLUMN_CLIENT_ID AS $MESSAGE_ALIAS_PREFIX$COLUMN_CLIENT_ID, " +
                    "$MESSAGES_TABLE_NAME.$COLUMN_CONTENT AS $MESSAGE_ALIAS_PREFIX$COLUMN_CONTENT, " +
                    "$MESSAGES_TABLE_NAME.$COLUMN_PROTO_BLOB AS $MESSAGE_ALIAS_PREFIX$COLUMN_PROTO_BLOB, " +
                    "$ASSETS_TABLE_NAME.$COLUMN_NAME AS $ASSET_ALIAS_PREFIX$COLUMN_NAME, " +
                    "$ASSETS_TABLE_NAME.$COLUMN_SIZE AS $ASSET_ALIAS_PREFIX$COLUMN_SIZE " +
                    "FROM $MESSAGES_TABLE_NAME " +
                    "LEFT JOIN $ASSETS_TABLE_NAME ON $MESSAGES_TABLE_NAME.$COLUMN_ASSET_ID = $ASSETS_TABLE_NAME.$COLUMN_ID " +
                    "WHERE $MESSAGES_TABLE_NAME.$COLUMN_CONVERSATION_ID = ?" +
                    "ORDER BY $MESSAGES_TABLE_NAME.$COLUMN_TIME ASC ", arrayOf(scalaConversation.id)
        )
        return try {
            if (!cursor.moveToFirst()) {
                emptyList()
            } else {
                val idIndex = cursor.getColumnIndex("$MESSAGE_ALIAS_PREFIX$COLUMN_ID")
                val timeIndex = cursor.getColumnIndex("$MESSAGE_ALIAS_PREFIX$COLUMN_TIME")
                val editTimeIndex = cursor.getColumnIndex("$MESSAGE_ALIAS_PREFIX$COLUMN_EDIT_TIME").orNullIfNegative()
                val userIdIndex = cursor.getColumnIndex("$MESSAGE_ALIAS_PREFIX$COLUMN_USER_ID")
                val clientIdIndex = cursor.getColumnIndex("$MESSAGE_ALIAS_PREFIX$COLUMN_CLIENT_ID")
                val contentIndex = cursor.getColumnIndex("$MESSAGE_ALIAS_PREFIX$COLUMN_CONTENT")
                val protoIndex = cursor.getColumnIndex("$MESSAGE_ALIAS_PREFIX$COLUMN_PROTO_BLOB")
                val assetNameIndex = cursor.getColumnIndex("$ASSET_ALIAS_PREFIX$COLUMN_NAME")
                val assetSizeIndex = cursor.getColumnIndex("$ASSET_ALIAS_PREFIX$COLUMN_SIZE")
                val accumulator = mutableListOf<ScalaMessageData>()
                do {
                    accumulator += ScalaMessageData(
                        id = cursor.getStringOrNull(idIndex).orEmpty(),
                        conversationId = scalaConversation.id,
                        conversationRemoteId = scalaConversation.remoteId,
                        conversationDomain = scalaConversation.domain,
                        time = cursor.getLong(timeIndex),
                        editTime = editTimeIndex?.let { cursor.getLongOrNull(it) },
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
        const val MESSAGE_ALIAS_PREFIX = "message_"
        const val ASSET_ALIAS_PREFIX = "asset_"
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
