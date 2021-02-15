package com.wire.android.feature.conversation.list.datasources

import androidx.lifecycle.asFlow
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.wire.android.feature.contact.datasources.local.ContactLocalDataSource
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.datasources.local.ConversationListItemEntity
import com.wire.android.feature.conversation.list.datasources.local.ConversationListLocalDataSource
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import kotlinx.coroutines.flow.Flow

class ConversationListDataSource(
    private val conversationListLocalDataSource: ConversationListLocalDataSource,
    private val contactLocalDataSource: ContactLocalDataSource,
    private val conversationListMapper: ConversationListMapper
) : ConversationListRepository {

    override fun conversationListInBatch(pageSize: Int): Flow<PagedList<ConversationListItem>> =
        conversationListLocalDataSource.conversationListInBatch()
            .map { fromListItemEntity(it) }
            .toLiveData(pageSize = pageSize)
            .asFlow()

    private fun fromListItemEntity(entity: ConversationListItemEntity): ConversationListItem {
        val profilePictures = entity.members.map { contactEntity ->
            contactLocalDataSource.profilePicture(contactEntity).fold({ null }) { it }
        }
        return conversationListMapper.fromEntity(entity, profilePictures)
    }

}
