package com.wire.android.feature.conversation.list.usecase

import androidx.paging.PagedList
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsPagingDelegate
import com.wire.android.feature.conversation.data.ConversationsRepository
import kotlinx.coroutines.flow.Flow

class GetConversationsUseCase(
    private val conversationsRepository: ConversationsRepository
) : ObservableUseCase<PagedList<Conversation>, GetConversationsParams> {

    override suspend fun run(params: GetConversationsParams): Flow<Either<Failure, PagedList<Conversation>>> =
        conversationsRepository.conversationsByBatch(params.pagingDelegate)
}

data class GetConversationsParams(val pagingDelegate: ConversationsPagingDelegate)
