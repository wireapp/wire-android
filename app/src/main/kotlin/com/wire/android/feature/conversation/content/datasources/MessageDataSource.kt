package com.wire.android.feature.conversation.content.datasources

import android.util.Base64
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MessageDataSource(
    private val messageLocalDataSource: MessageLocalDataSource,
    private val messageMapper: MessageMapper,
    private val cryptoBoxClient: CryptoBoxClient
) : MessageRepository {

    override suspend fun decryptMessage(message: Message) {
        message.clientId?.let { _ ->
            val decodedContent = Base64.decode(message.content, Base64.DEFAULT)
            decodedContent?.let {
                val cryptoSessionId = messageMapper.cryptoSessionFromMessage(message)
                val encryptedMessage = messageMapper.encryptedMessageFromDecodedContent(it)
                cryptoBoxClient.decryptMessage(cryptoSessionId, encryptedMessage) { plainMessage ->
                    val decryptedMessage = messageMapper.toDecryptedMessage(message, plainMessage)
                    GlobalScope.launch(Dispatchers.IO) {
                        save(decryptedMessage)
                    }
                    Either.Right(Unit)
                }
            }
        }
    }

    private suspend fun save(message: Message): Either<Failure, Unit> {
        val messageEntity = messageMapper.fromMessageToEntity(message)
        return messageLocalDataSource.save(messageEntity)
    }

    override suspend fun conversationMessages(conversationId: String): Flow<List<Message>> =
        messageLocalDataSource.messagesByConversationId(conversationId).map { messages ->
            messages.map { messageMapper.fromEntityToMessage(it) }
        }
}
