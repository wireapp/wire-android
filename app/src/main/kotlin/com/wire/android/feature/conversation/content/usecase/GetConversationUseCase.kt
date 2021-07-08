package com.wire.android.feature.conversation.content.usecase

import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.ui.MessageAndContact
import kotlinx.coroutines.flow.Flow

class GetConversationUseCase(private val messageRepository: MessageRepository) :
    ObservableUseCase<List<MessageAndContact>, GetConversationUseCaseParams> {

    override suspend fun run(params: GetConversationUseCaseParams): Flow<List<MessageAndContact>> =
        messageRepository.conversationMessages(params.conversationId)
}

data class GetConversationUseCaseParams(val conversationId: String)
