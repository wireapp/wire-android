package com.wire.android.feature.conversation.list.usecase

import androidx.paging.PagedList
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsPagingDelegate
import com.wire.android.feature.conversation.data.ConversationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class GetConversationsUseCase(private val conversationsRepository: ConversationsRepository) {

    //TODO: create some sort of ObservableUseCase and a corr. executor
    operator fun invoke(
        scope: CoroutineScope,
        params: GetConversationsParams,
        onResult: (Either<Failure, PagedList<Conversation>>) -> Unit
    ) {
        scope.launch {
            conversationsRepository.conversationsByBatch(ConversationsPagingDelegate(scope, params.size)).collect {
                onResult(it)
            }
        }
    }
}

data class GetConversationsParams(val size: Int)
