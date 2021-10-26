package com.wire.android.feature.conversation.content

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.ui.CombinedMessageContact
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun receiveEncryptedMessage(message: EncryptedMessageEnvelope)
    suspend fun messageById(id: String): Either<Failure, Message>
    suspend fun conversationMessages(conversationId: String): Flow<List<CombinedMessageContact>>
    suspend fun latestUnreadMessages(conversationId: String): Either<Failure, List<CombinedMessageContact>>
}
