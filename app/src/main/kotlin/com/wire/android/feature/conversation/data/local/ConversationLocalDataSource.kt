package com.wire.android.feature.conversation.data.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService
import com.wire.android.feature.conversation.list.datasources.local.ConversationDao
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity

class ConversationLocalDataSource(private val conversationDao: ConversationDao) : DatabaseService {

    suspend fun saveConversations(conversations: List<ConversationEntity>): Either<Failure, Unit> = request {
        conversationDao.insertAll(conversations)
    }
}
