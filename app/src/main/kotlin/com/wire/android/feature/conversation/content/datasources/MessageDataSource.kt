package com.wire.android.feature.conversation.content.datasources

import android.util.Log
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.CryptoClientId
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageDataSource(
    private val messageLocalDataSource: MessageLocalDataSource,
    private val messageMapper: MessageMapper,
    private val cryptoBoxClient: CryptoBoxClient
) : MessageRepository {

    override suspend fun decryptMessage(message: Message) {
        cryptoBoxClient.decryptMessage(
            CryptoSessionId(UserId(message.userId), CryptoClientId(message.clientId)),
            EncryptedMessage(message.content.toByteArray())) {
                onDecrypt(it)
            }
         }

    private fun onDecrypt(plainMessage : PlainMessage) : Either<Failure, Unit> {
        Log.d("TAG", "onDecrypt: ${plainMessage.data.toString(Charsets.UTF_8)}")
        return Either.Right(Unit)
    }

    override suspend fun save(message: Message): Either<Failure, Unit> {
        val messageEntity = messageMapper.fromMessageToEntity(message)
        return messageLocalDataSource.save(messageEntity)
    }

    override suspend fun conversationMessages(conversationId: String): Flow<List<Message>> =
        messageLocalDataSource.messagesByConversationId(conversationId).map { messages ->
            messages.map { messageMapper.fromEntityToMessage(it) }
        }
}
