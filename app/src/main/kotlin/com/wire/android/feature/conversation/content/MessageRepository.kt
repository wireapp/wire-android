package com.wire.android.feature.conversation.content

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun decryptMessage(message: Message)
    suspend fun save(message: Message): Either<Failure, Unit>
    suspend fun conversationMessages(conversationId: String): Flow<List<Message>>
}
