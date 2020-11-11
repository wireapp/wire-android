package com.wire.android.feature.conversation.list.usecase

import com.wire.android.feature.conversation.data.ConversationsPagingDelegate
import com.wire.android.feature.conversation.data.ConversationsRepository
import kotlinx.coroutines.CoroutineScope

class GetConversationsUseCase(private val conversationsRepository: ConversationsRepository) {

    //TODO: create some sort of UseCase which can return LiveData
    operator fun invoke(scope: CoroutineScope, params: GetConversationsParams) =
        conversationsRepository.conversationsByBatch(ConversationsPagingDelegate(scope, params.size)) //TODO inject delegate/use provider
}

data class GetConversationsParams(val size: Int)
