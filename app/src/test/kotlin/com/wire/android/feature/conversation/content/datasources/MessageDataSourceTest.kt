package com.wire.android.feature.conversation.content.datasources

import android.util.Base64
import com.wire.android.UnitTest
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.datasources.local.MessageEntity
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import io.mockk.*
import io.mockk.impl.annotations.MockK
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
    private lateinit var cryptoBoxClient: CryptoBoxClient

    @Before
    fun setUp() {
        messageDataSource = MessageDataSource(messageLocalDataSource, messageMapper, cryptoBoxClient)
    }

    @Test
    fun `given decryptMessage is called, when clientId is null, then do not decrypt message`() {
        val message = mockk<Message>().also {
            every { it.clientId } returns null
        }
        val cryptoSession = mockk<CryptoSessionId>()
        val encryptedMessage = mockk<EncryptedMessage>()

        runBlocking { messageDataSource.decryptMessage(message) }

        coVerify(inverse = true) { cryptoBoxClient.decryptMessage(cryptoSession, encryptedMessage){ _ -> Either.Right(Unit) }}
    }

    @Test
    fun `given decryptMessage is called, when decoded content is null, then do not decrypt message`() {
        mockkStatic(Base64::class)
        every { Base64.decode(TEST_CONTENT, Base64.DEFAULT) } returns "null".toByteArray()
        val message = mockk<Message>().also {
            every { it.clientId } returns TEST_CLIENT_ID
            every { it.content } returns TEST_CONTENT
            every { it.userId } returns TEST_CONTENT
        }
        val cryptoSession = mockk<CryptoSessionId>()
        val encryptedMessage = mockk<EncryptedMessage>()

        runBlocking { messageDataSource.decryptMessage(message) }

        coVerify(inverse = true) { cryptoBoxClient.decryptMessage(cryptoSession, encryptedMessage){ _ -> Either.Right(Unit) }}
    }

    @Test
    fun `given decryptMessage is called, when cryptoSession is invalid, then do not decrypt message`() {
       //TODO
    }

    @Test
    fun `given conversationMessages is called, when messageLocalDataSource emits messages, then propagates mapped items`(){
        val conversationId = "conversation-id"
        val messageEntity = mockk<MessageEntity>()
        val message = mockk<Message>()
        every { messageMapper.fromEntityToMessage(messageEntity) } returns message
        coEvery { messageLocalDataSource.messagesByConversationId(conversationId) } returns flowOf(listOf(messageEntity))

        runBlocking {
            val result = messageDataSource.conversationMessages(conversationId)
            with(result.first()){
                size shouldBeEqualTo 1
                get(0) shouldBeEqualTo message
            }
        }
    }

    companion object {
        private const val TEST_CLIENT_ID = "client-id"
        private const val TEST_CONTENT = "This-is-a-content"
    }
}
