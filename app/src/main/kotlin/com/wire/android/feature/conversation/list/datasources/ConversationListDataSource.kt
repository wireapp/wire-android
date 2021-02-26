package com.wire.android.feature.conversation.list.datasources

import androidx.lifecycle.asFlow
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.contact.datasources.local.ContactLocalDataSource
import com.wire.android.feature.conversation.ConversationType
import com.wire.android.feature.conversation.data.ConversationTypeMapper
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.datasources.local.ConversationListItemEntity
import com.wire.android.feature.conversation.list.datasources.local.ConversationListLocalDataSource
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import kotlinx.coroutines.flow.Flow

class ConversationListDataSource(
    private val conversationListLocalDataSource: ConversationListLocalDataSource,
    private val contactLocalDataSource: ContactLocalDataSource,
    private val conversationListMapper: ConversationListMapper,
    private val conversationTypeMapper: ConversationTypeMapper
) : ConversationListRepository {

    override fun conversationListInBatch(pageSize: Int, excludeType: ConversationType): Flow<PagedList<ConversationListItem>> =
        conversationListLocalDataSource.conversationListInBatch(
            excludeType = conversationTypeMapper.toIntValue(excludeType)
        ).map { fromListItemEntity(it) }
            .toLiveData(pageSize = pageSize)
            .asFlow()

    private fun fromListItemEntity(entity: ConversationListItemEntity): ConversationListItem {
        val profilePictures = entity.members.map { contactEntity ->
            contactLocalDataSource.profilePicture(contactEntity).fold({ null }) { it }
        }
        return conversationListMapper.fromEntity(entity, profilePictures)
    }

    override suspend fun conversationListInBatch(start: Int, count: Int): Either<Failure, List<ConversationListItem>> =
        conversationListLocalDataSource.conversationListInBatch(start = start, count = count).map {
            it.map { conversationListMapper.fromEntity(it, emptyList()) }
        }
}
