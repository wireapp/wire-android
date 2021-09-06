package com.wire.android.shared.notification.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.conversation.data.ConversationRepository

//TODO test to be done in next PR
class ShouldDisplayNotificationUseCase(private val conversationRepository: ConversationRepository) :
    UseCase<Boolean, ShouldDisplayNotificationUseCaseParams> {

    override suspend fun run(params: ShouldDisplayNotificationUseCaseParams): Either<Failure, Boolean> =
        conversationRepository.currentOpenedConversationId().fold({
            Either.Left(it)
        }, {
            if (it == params.conversationId)
                Either.Right(false)
            else Either.Right(true)
        })!!

}

data class ShouldDisplayNotificationUseCaseParams(val conversationId: String)
