package com.wire.android.feature.conversation.content

import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.ui.CombinedMessageContact
import com.wire.android.feature.messaging.ChatMessageEnvelope
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun receiveEncryptedMessage(message: EncryptedMessageEnvelope)
    suspend fun messageById(id: String): Either<Failure, Message>
    suspend fun markMessageAsSent(messageId: String): Either<Failure, Unit>
    suspend fun sendMessageEnvelope(
        conversationId: String,
        envelope: ChatMessageEnvelope
    ): Either<SendMessageFailure, Unit>

    suspend fun doesCryptoSessionExists(selfUserId: String, contactUserId: String, contactClientId: String): Either<Failure, Boolean>

    suspend fun establishCryptoSession(
        selfUserId: String,
        contactUserId: String,
        contactClientId: String,
        preKey: PreKey
    ): Either<Failure, Unit>

    suspend fun encryptMessageContent(
        senderUserId: String,
        receiverUserId: String,
        receiverClientId: String,
        messageId: String,
        content: Content
    ): Either<Failure, EncryptedMessage>

    suspend fun storeOutgoingMessage(message: Message): Either<Failure, Unit>

    suspend fun conversationMessages(conversationId: String): Flow<List<CombinedMessageContact>>
    suspend fun latestUnreadMessages(conversationId: String): Either<Failure, List<CombinedMessageContact>>
}
