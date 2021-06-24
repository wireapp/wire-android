package com.wire.android.feature.conversation.content.datasources

import com.wire.android.UnitTest
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.datasources.local.MessageEntity
import com.wire.android.feature.conversation.content.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.content.mapper.MessageMapper
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
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
    fun `given save is called, when localDataSource returns success, then returns success`() {
        val message = mockk<Message>()
        val messageEntity = mockk<MessageEntity>()
        every { messageMapper.fromMessageToEntity(message) } returns messageEntity
        coEvery { messageLocalDataSource.save(messageEntity) } returns Either.Right(Unit)

        val result = runBlocking { messageDataSource.save(message) }

        result shouldSucceed { it shouldBe Unit }
        coVerify(exactly = 1) { messageLocalDataSource.save(messageEntity) }
    }

    @Test
    fun `given save is called, when localDataSource returns a failure, then returns that failure`() {
        val message = mockk<Message>()
        val messageEntity = mockk<MessageEntity>()
        val failure = DatabaseFailure()
        every { messageMapper.fromMessageToEntity(message) } returns messageEntity
        coEvery { messageLocalDataSource.save(messageEntity) } returns Either.Left(failure)

        val result = runBlocking { messageDataSource.save(message) }

        result shouldFail { it shouldBe failure }
        coVerify(exactly = 1) { messageLocalDataSource.save(messageEntity) }
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
}
