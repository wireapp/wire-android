package com.wire.android.feature.conversation.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.conversation.data.ConversationRepository

class UpdateCurrentConversationIdUseCase(private val conversationRepository: ConversationRepository) :
    UseCase<Unit, UpdateCurrentConversationUseCaseParams> {
    override suspend fun run(params: UpdateCurrentConversationUseCaseParams): Either<Failure, Unit> =
        conversationRepository.updateCurrentConversationId(params.conversationId)
}

data class UpdateCurrentConversationUseCaseParams(val conversationId: String)
