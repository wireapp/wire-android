package com.wire.android.feature.conversation.list.usecase

import androidx.paging.PagedList
import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import kotlinx.coroutines.flow.Flow

class GetConversationListUseCase(
    private val conversationListRepository: ConversationListRepository
) : ObservableUseCase<PagedList<ConversationListItem>, GetConversationListUseCaseParams> {

    override suspend fun run(params: GetConversationListUseCaseParams): Flow<PagedList<ConversationListItem>> =
        conversationListRepository.conversationListInBatch(params.pageSize)
}

data class GetConversationListUseCaseParams(val pageSize: Int)
