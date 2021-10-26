package com.wire.android.feature.conversation.content.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService
import kotlinx.coroutines.flow.Flow

class MessageLocalDataSource(private val messageDao: MessageDao) : DatabaseService {
    suspend fun save(message: MessageEntity): Either<Failure, Unit> = request { messageDao.insert(message) }

    fun messagesByConversationId(conversationId: String): Flow<List<CombinedMessageContactEntity>> =
        messageDao.messagesByConversationId(conversationId)

    suspend fun latestUnreadMessagesByConversationId(conversationId: String, size: Int) = request {
        messageDao.latestUnreadMessagesByConversationId(conversationId, size)
    }

    suspend fun messageById(messageId: String) = request {
        messageDao.messageById(messageId)
    }
}
