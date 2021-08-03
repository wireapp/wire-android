package com.wire.android.feature.conversation.content.datasources

import android.util.Base64
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.conversation.content.EncryptedMessageEnvelope
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import com.wire.android.feature.conversation.content.ui.CombinedMessageContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MessageDataSource(
    private val messageLocalDataSource: MessageLocalDataSource,
    private val messageMapper: MessageMapper,
    private val contactMapper: ContactMapper,
    private val cryptoBoxClient: CryptoBoxClient
) : MessageRepository {

    override suspend fun receiveEncryptedMessage(message: EncryptedMessageEnvelope): Unit = coroutineScope {
        message.clientId?.let { _ ->
            val decodedContent = Base64.decode(message.content, Base64.DEFAULT)
            decodedContent?.let {
                val cryptoSessionId = messageMapper.cryptoSessionFromEncryptedEnvelope(message)
                val encryptedMessage = messageMapper.encryptedMessageFromDecodedContent(it)
                cryptoBoxClient.decryptMessage(cryptoSessionId, encryptedMessage) { plainMessage ->
                    val decryptedMessage = messageMapper.toDecryptedMessage(message, plainMessage)
                    launch(Dispatchers.IO) { save(decryptedMessage) }
                    Either.Right(Unit)
                }
            }
        }
    }

    private suspend fun save(message: Message): Either<Failure, Unit> {
        val messageEntity = messageMapper.fromMessageToEntity(message)
        return messageLocalDataSource.save(messageEntity)
    }

    override suspend fun conversationMessages(conversationId: String): Flow<List<CombinedMessageContact>> =
        messageLocalDataSource.messagesByConversationId(conversationId).map { messagesWithContact ->
            messagesWithContact.map {
                CombinedMessageContact(
                    messageMapper.fromEntityToMessage(it.messageEntity),
                    contactMapper.fromContactEntity(it.contactEntity)
                )
            }
        }
}
