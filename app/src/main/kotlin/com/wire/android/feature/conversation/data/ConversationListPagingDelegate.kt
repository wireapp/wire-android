package com.wire.android.feature.conversation.data

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.wire.android.feature.conversation.list.datasources.ConversationListMapper
import com.wire.android.feature.conversation.list.datasources.local.ConversationListDao
import com.wire.android.feature.conversation.list.ui.ConversationListItem

class ConversationListPagingDelegate(dao: ConversationListDao, private val conversationListMapper: ConversationListMapper) {

    private val dataFactory = dao.conversationListItemsInBatch().map {
        conversationListMapper.fromEntity(it)
    }

    fun conversationList(pageSize: Int, loadNextPage: (ConversationListItem?) -> Unit): LiveData<PagedList<ConversationListItem>> {
        val boundaryCallback = object : PagedList.BoundaryCallback<ConversationListItem>() {

            override fun onZeroItemsLoaded() = loadNextPage(null)

            override fun onItemAtEndLoaded(itemAtEnd: ConversationListItem) = loadNextPage(itemAtEnd)
        }

        return dataFactory.toLiveData(pageSize = pageSize, boundaryCallback = boundaryCallback)
    }
}
