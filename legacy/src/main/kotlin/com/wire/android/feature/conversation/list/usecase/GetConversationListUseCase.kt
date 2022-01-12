package com.wire.android.feature.conversation.list.usecase

import androidx.paging.PagingData
import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.feature.conversation.Self
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import kotlinx.coroutines.flow.Flow

class GetConversationListUseCase(
    private val conversationListRepository: ConversationListRepository
) : ObservableUseCase<PagingData<ConversationListItem>, GetConversationListUseCaseParams> {

    override suspend fun run(params: GetConversationListUseCaseParams): Flow<PagingData<ConversationListItem>> =
        conversationListRepository.conversationListInBatch(params.pageSize, excludeType = Self)
}

data class GetConversationListUseCaseParams(val pageSize: Int)
