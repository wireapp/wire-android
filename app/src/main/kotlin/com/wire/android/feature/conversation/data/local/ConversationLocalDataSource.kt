package com.wire.android.feature.conversation.data.local

import androidx.paging.DataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService
import com.wire.android.feature.conversation.list.datasources.local.ConversationDao
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity

class ConversationLocalDataSource(private val conversationDao: ConversationDao) : DatabaseService {

    fun conversationsDataFactory() : DataSource.Factory<Int, ConversationEntity> = conversationDao.conversationsInBatch()

    suspend fun saveConversations(conversations: List<ConversationEntity>): Either<Failure, Unit> = request {
        conversationDao.insertAll(conversations)
    }
}
