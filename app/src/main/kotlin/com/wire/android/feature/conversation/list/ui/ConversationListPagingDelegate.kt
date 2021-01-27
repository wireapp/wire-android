package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.wire.android.feature.conversation.list.ConversationListRepository

class ConversationListPagingDelegate(private val conversationListRepository: ConversationListRepository) {

    fun conversationList(pageSize: Int): LiveData<PagedList<ConversationListItem>> =
        conversationListRepository.conversationListDataSourceFactory().toLiveData(pageSize = pageSize)
}
