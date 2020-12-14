package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.wire.android.feature.conversation.list.ConversationListRepository

class ConversationListPagingDelegate(private val conversationListRepository: ConversationListRepository) {

    fun conversationList(pageSize: Int, loadNextPage: (ConversationListItem?) -> Unit): LiveData<PagedList<ConversationListItem>> {
        val boundaryCallback = object : PagedList.BoundaryCallback<ConversationListItem>() {

            override fun onZeroItemsLoaded() = loadNextPage(null)

            override fun onItemAtEndLoaded(itemAtEnd: ConversationListItem) = loadNextPage(itemAtEnd)
        }

        return conversationListRepository.conversationListDataSourceFactory()
            .toLiveData(pageSize = pageSize, boundaryCallback = boundaryCallback)
    }
}
