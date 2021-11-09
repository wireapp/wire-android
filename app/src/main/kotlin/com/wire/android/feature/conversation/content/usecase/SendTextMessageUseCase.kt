package com.wire.android.feature.conversation.content.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.feature.conversation.ConversationID
import com.wire.android.feature.conversation.content.Content
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.Pending
import com.wire.android.feature.conversation.content.domain.SendMessageService
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.OffsetDateTime
import java.util.UUID

class SendTextMessageUseCase(
    private val sessionRepository: SessionRepository,
    private val sendMessageService: SendMessageService
) : ObservableUseCase<Either<Failure, Unit>, SendTextMessageUseCaseParams> {

    override suspend fun run(params: SendTextMessageUseCaseParams): Flow<Either<Failure, Unit>> = flow {
        suspending {
            //TODO Multi-session support
            sessionRepository.currentSession().map { session ->
                val content = Content.Text(params.text)
                val message = Message(
                    UUID.randomUUID().toString(), params.conversationId.value, session.userId, session.clientId, content,
                    Pending, OffsetDateTime.now(), false
                )
                sendMessageService.sendOrScheduleNewMessage(message)
            }
        }
    }
}

data class SendTextMessageUseCaseParams(val conversationId: ConversationID, val text: String)
