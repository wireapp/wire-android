package com.wire.android.feature.conversation.content.datasources

import android.util.Base64
import com.wire.android.UnitTest
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.datasources.local.MessageEntity
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import com.wire.android.core.exception.Failure
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.conversation.content.datasources.local.MessageAndContactEntity
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.mockk.coVerify
import io.mockk.every
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class MessageDataSourceTest : UnitTest() {

    private lateinit var messageDataSource: MessageDataSource

    @MockK
    private lateinit var messageLocalDataSource: MessageLocalDataSource

    @MockK
    private lateinit var messageMapper: MessageMapper

    @MockK
    private lateinit var contactMapper: ContactMapper

    @MockK
    private lateinit var cryptoBoxClient: CryptoBoxClient

    @MockK
    private lateinit var cryptoSession: CryptoSessionId

    @MockK
    private lateinit var encryptedMessage: EncryptedMessage

    @Before
    fun setUp() {
        messageDataSource = MessageDataSource(messageLocalDataSource, messageMapper, contactMapper, cryptoBoxClient)
    }

    @Test
    fun `given decryptMessage is called, when clientId is null, then do not decrypt message`() {
        val message = mockk<Message>().also {
            every { it.clientId } returns null
        }

        runBlocking { messageDataSource.decryptMessage(message) }

        coVerify(inverse = true) { cryptoBoxClient.decryptMessage(cryptoSession, encryptedMessage){ _ -> Either.Right(Unit) }}
    }

    @Test
    fun `given decryptMessage is called, when decoded content is null, then do not decrypt message`() {
        mockkStatic(Base64::class)
        every { Base64.decode(TEST_CONTENT, Base64.DEFAULT) } returns null
        val message = mockk<Message>().also {
            every { it.clientId } returns TEST_CLIENT_ID
            every { it.content } returns TEST_CONTENT
        }

        runBlocking { messageDataSource.decryptMessage(message) }

        coVerify(inverse = true) { cryptoBoxClient.decryptMessage(cryptoSession, encryptedMessage){ _ -> Either.Right(Unit) }}
    }

    @Test
    fun `given decryptMessage is called, when message is valid, then it should be decrypted`() {
        val plainMessage = PlainMessage(byteArrayOf())
        mockkStatic(Base64::class)
        every { Base64.decode(TEST_CONTENT, Base64.DEFAULT) } returns byteArrayOf()
        val message = mockk<Message>().also {
            every { it.clientId } returns TEST_CLIENT_ID
            every { it.senderUserId } returns TEST_USER_ID
            every { it.content } returns TEST_CONTENT
        }

        every { messageMapper.cryptoSessionFromMessage(message) } returns cryptoSession
        every { messageMapper.encryptedMessageFromDecodedContent(byteArrayOf()) } returns encryptedMessage
        val lambdaSlot = slot<((PlainMessage) -> Either<Failure, Unit>)>()

        runBlocking { messageDataSource.decryptMessage(message) }

        coVerify { cryptoBoxClient.decryptMessage(cryptoSession, encryptedMessage, capture(lambdaSlot)) }
        lambdaSlot.captured.invoke(plainMessage)
        verify(exactly = 1) { messageMapper.cryptoSessionFromMessage(message) }
        verify(exactly = 1) { messageMapper.encryptedMessageFromDecodedContent(byteArrayOf()) }
        verify(exactly = 1) { messageMapper.toDecryptedMessage(message, plainMessage) }
        coVerify(exactly = 1) { messageLocalDataSource.save(any()) }
    }

    @Test
    fun `given conversationMessages is called, when messageLocalDataSource emits messages, then propagates mapped items`(){
        val conversationId = "conversation-id"
        val contactEntity = mockk<ContactEntity>()
        val messageEntity = mockk<MessageEntity>()
        val messageAndContactEntity = mockk<MessageAndContactEntity>().also {
            every { it.messageEntity } returns messageEntity
            every { it.contactEntity } returns contactEntity
        }
        val message = mockk<Message>()
        val contact = mockk<Contact>()
        every { messageMapper.fromEntityToMessage(messageEntity) } returns message
        every { contactMapper.fromContactEntity(contactEntity) } returns contact
        coEvery { messageLocalDataSource.messagesByConversationId(conversationId) } returns flowOf(listOf(messageAndContactEntity))

        runBlocking {
            val result = messageDataSource.conversationMessages(conversationId)

            with(result.first()){
                size shouldBeEqualTo 1
                get(0).message shouldBeEqualTo message
                get(0).contact shouldBeEqualTo contact
            }
        }
    }

    companion object {
        private const val TEST_CLIENT_ID = "client-id"
        private const val TEST_USER_ID = "user-id"
        private const val TEST_CONTENT = "This-is-a-content"
    }
}
