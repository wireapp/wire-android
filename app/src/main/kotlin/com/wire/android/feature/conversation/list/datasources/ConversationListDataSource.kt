package com.wire.android.feature.conversation.list.datasources

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.conversation.ConversationType
import com.wire.android.feature.conversation.data.ConversationTypeMapper
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.datasources.local.ConversationListLocalDataSource
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationListDataSource(
    private val conversationListLocalDataSource: ConversationListLocalDataSource,
    private val conversationListMapper: ConversationListMapper,
    private val conversationTypeMapper: ConversationTypeMapper
) : ConversationListRepository {

    override fun conversationListInBatch(
        pageSize: Int,
        excludeType: ConversationType
    ): Flow<PagingData<ConversationListItem>> {
        val type = conversationTypeMapper.toIntValue(excludeType)
        return Pager(config = PagingConfig(pageSize = pageSize)) {
            conversationListLocalDataSource.conversationListInBatch(excludeType = type)
        }.flow.map { it.map { conversationItem -> conversationListMapper.fromEntity(conversationItem) } }
    }

    override suspend fun conversationListInBatch(start: Int, count: Int): Either<Failure, List<ConversationListItem>> =
        conversationListLocalDataSource.conversationListInBatch(start = start, count = count).map { items ->
            items.map { conversationListMapper.fromEntity(it) }
        }
}
