package com.wire.android.feature.messaging.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.messaging.MessageContent

interface MessageRepository {

    suspend fun sendMessage(conversationId: String, content: MessageContent, recipientUserIds: List<String>): Either<Failure, Unit>

}
