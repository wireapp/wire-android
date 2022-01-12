package com.wire.android.feature.conversation.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.conversation.data.ConversationRepository

class ResetCurrentConversationIdUseCase(private val conversationRepository: ConversationRepository) :
    UseCase<Unit, Unit> {
    override suspend fun run(params: Unit): Either<Failure, Unit> =
        conversationRepository.restCurrentConversationId()
}
