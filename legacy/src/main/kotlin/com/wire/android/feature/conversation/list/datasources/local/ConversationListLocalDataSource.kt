package com.wire.android.feature.conversation.list.datasources.local

import androidx.paging.PagingSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class ConversationListLocalDataSource(private val conversationListDao: ConversationListDao) : DatabaseService {

    fun conversationListInBatch(excludeType: Int): PagingSource<Int, ConversationListItemEntity> =
        conversationListDao.conversationListItemsInBatch(excludeType)

    suspend fun conversationListInBatch(start: Int, count: Int): Either<Failure, List<ConversationListItemEntity>> = request {
        conversationListDao.conversationListItemsInBatch(start = start, count = count)
    }
}
