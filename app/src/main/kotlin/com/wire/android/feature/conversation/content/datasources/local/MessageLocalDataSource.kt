package com.wire.android.feature.conversation.content.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.storage.db.DatabaseService
import com.wire.android.feature.conversation.content.Sent
import com.wire.android.feature.conversation.content.mapper.MessageStateMapper
import kotlinx.coroutines.flow.Flow

class MessageLocalDataSource(
    private val messageDao: MessageDao,
    private val messageStateMapper: MessageStateMapper
) : DatabaseService {
    suspend fun save(message: MessageEntity): Either<Failure, Unit> = request { messageDao.insert(message) }

    fun messagesByConversationId(conversationId: String): Flow<List<CombinedMessageContactEntity>> =
        messageDao.messagesByConversationId(conversationId)

    suspend fun latestUnreadMessagesByConversationId(conversationId: String, size: Int) = request {
        messageDao.latestUnreadMessagesByConversationId(conversationId, size)
    }

    suspend fun messageById(messageId: String) = request {
        messageDao.messageById(messageId)
    }

    suspend fun markMessageAsSent(messageId: String) = suspending {
        request {
            messageDao.messageById(messageId)
                .copy(state = messageStateMapper.fromValueToString(Sent))
        }.flatMap {
            save(it)
        }
    }

}
