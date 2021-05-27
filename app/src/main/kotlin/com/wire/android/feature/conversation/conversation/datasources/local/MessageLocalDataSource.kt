package com.wire.android.feature.conversation.conversation.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService
import kotlinx.coroutines.flow.Flow

class MessageLocalDataSource(private val messageDao: MessageDao) : DatabaseService {
    suspend fun save(message: MessageEntity): Either<Failure, Unit> = request { messageDao.insert(message) }

    fun messagesByConversationId(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.messagesByConversationId(conversationId)
}
