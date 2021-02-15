package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.paging.PagedList
import com.wire.android.feature.conversation.list.ConversationListRepository

//TODO: convert to use case
class ConversationListPagingDelegate(private val conversationListRepository: ConversationListRepository) {

    fun conversationList(pageSize: Int): LiveData<PagedList<ConversationListItem>> =
        conversationListRepository.conversationListInBatch(pageSize).asLiveData()
}
