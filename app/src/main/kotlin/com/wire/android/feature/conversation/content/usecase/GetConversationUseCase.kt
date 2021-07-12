package com.wire.android.feature.conversation.content.usecase

import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.ui.CombinedMessageContact
import kotlinx.coroutines.flow.Flow

class GetConversationUseCase(private val messageRepository: MessageRepository) :
    ObservableUseCase<List<CombinedMessageContact>, GetConversationUseCaseParams> {

    override suspend fun run(params: GetConversationUseCaseParams): Flow<List<CombinedMessageContact>> =
        messageRepository.conversationMessages(params.conversationId)
}

data class GetConversationUseCaseParams(val conversationId: String)
