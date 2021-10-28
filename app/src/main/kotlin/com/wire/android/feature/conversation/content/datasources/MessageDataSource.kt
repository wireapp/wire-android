package com.wire.android.feature.conversation.content.datasources

import android.util.Base64
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.CryptoClientId
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.conversation.content.Content
import com.wire.android.feature.conversation.content.EncryptedMessageEnvelope
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageContentMapper
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import com.wire.android.feature.conversation.content.ui.CombinedMessageContact
import com.wire.android.shared.user.QualifiedId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageDataSource(
    private val messageLocalDataSource: MessageLocalDataSource,
    private val messageMapper: MessageMapper,
    private val contentMapper: MessageContentMapper,
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
                    save(decryptedMessage)
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

    override suspend fun latestUnreadMessages(conversationId: String): Either<Failure, List<CombinedMessageContact>> =
        messageLocalDataSource.latestUnreadMessagesByConversationId(conversationId, MESSAGES_SIZE).map { messagesWithContact ->
            messagesWithContact.map {
                CombinedMessageContact(
                    messageMapper.fromEntityToMessage(it.messageEntity),
                    contactMapper.fromContactEntity(it.contactEntity)
                )
            }
        }

    override suspend fun doesCryptoSessionExists(
        selfUserId: String,
        contactUserId: String,
        contactClientId: String
    ): Either<Failure, Boolean> {
        //TODO Use selfUserId to fetch the correct CryptoBoxClient (support multi-session)
        //TODO Use actual qualified id when handling federation
        val cryptoSession = CryptoSessionId(QualifiedId(FIXED_DOMAIN, contactUserId), CryptoClientId(contactClientId))
        return cryptoBoxClient.doesSessionExists(cryptoSession)
    }

    override suspend fun establishCryptoSession(
        selfUserId: String,
        contactUserId: String,
        contactClientId: String,
        preKey: PreKey
    ): Either<Failure, Unit> {
        //TODO Use selfUserId to fetch the correct CryptoBoxClient (support multi-session)
        //TODO Use actual qualified id when handling federation
        val cryptoSession = CryptoSessionId(QualifiedId(FIXED_DOMAIN, contactUserId), CryptoClientId(contactClientId))
        return cryptoBoxClient.createSessionIfNeeded(cryptoSession, preKey)
    }

    override suspend fun encryptMessageContent(
        senderUserId: String,
        receiverUserId: String,
        receiverClientId: String,
        messageId: String,
        content: Content
    ): Either<Failure, EncryptedMessage> {
        //TODO Use actual qualified id when handling federation
        val cryptoSession = CryptoSessionId(QualifiedId(FIXED_DOMAIN, receiverUserId), CryptoClientId(receiverClientId))

        val plainMessage = contentMapper.fromContentToPlainMessage(messageId, content)

        val completable = CompletableDeferred<EncryptedMessage>()

        return suspending {
            cryptoBoxClient.encryptMessage(cryptoSession, plainMessage) { encryptedMessage ->
                completable.complete(encryptedMessage)
                Either.Right(Unit)
            }.flatMap {
                Either.Right(completable.await())
            }
        }
    }

    override suspend fun messageById(id: String): Either<Failure, Message> =
        messageLocalDataSource.messageById(id)
            .map(messageMapper::fromEntityToMessage)

    override suspend fun storeOutgoingMessage(message: Message): Either<Failure, Unit> {
        val entity = messageMapper.fromMessageToEntity(message)
        return messageLocalDataSource.save(entity)
    }

    companion object {
        private const val MESSAGES_SIZE = 10
        private const val FIXED_DOMAIN = "domain"
    }
}
