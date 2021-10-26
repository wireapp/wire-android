package com.wire.android.feature.conversation.content.datasources.local

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import java.sql.SQLException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.any
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class MessageLocalDataSourceTest : UnitTest() {

    private lateinit var messageLocalDataSource: MessageLocalDataSource

    @MockK
    private lateinit var messageDao: MessageDao

    @Before
    fun setUp() {
        messageLocalDataSource = MessageLocalDataSource(messageDao)
    }

    @Test
    fun `given save is called, when dao insertion is successful, then returns success`() {
        val messageEntity = mockk<MessageEntity>()
        coEvery { messageDao.insert(messageEntity) } returns Unit

        val result = runBlocking { messageLocalDataSource.save(messageEntity) }

        result shouldSucceed { it shouldBe Unit }
        coVerify(exactly = 1) { messageDao.insert(messageEntity) }
    }

    @Test
    fun `given save is called, when dao insertion fails, then propagates failure`() {
        val messageEntity = mockk<MessageEntity>()
        coEvery { messageDao.insert(messageEntity) } throws SQLException()

        val result = runBlocking { messageLocalDataSource.save(messageEntity) }

        result shouldFail { }
        coVerify(exactly = 1) { messageDao.insert(messageEntity) }
    }

    @Test
    fun `given messagesByConversationId is called, when no message exists, then emits empty list`() {
        val conversationId = "conversationId"
        coEvery { messageDao.messagesByConversationId(conversationId) } returns flowOf(listOf())

        runBlocking {
            val result = messageLocalDataSource.messagesByConversationId(conversationId)

            result.first().size shouldBeEqualTo 0
        }
    }

    @Test
    fun `given messagesByConversationId is called, when dao emits some messages, then emits a list of messages`() {
        val conversationId = "conversationId"
        val combinedMessageContactEntity1 = mockk<CombinedMessageContactEntity>()
        val combinedMessageContactEntity2 = mockk<CombinedMessageContactEntity>()

        coEvery { messageDao.messagesByConversationId(conversationId) } returns
                flowOf(listOf(combinedMessageContactEntity1, combinedMessageContactEntity2))

        runBlocking {
            val result = messageLocalDataSource.messagesByConversationId(conversationId)

            with(result.first()) {
                size shouldBeEqualTo 2
                get(0) shouldBeEqualTo combinedMessageContactEntity1
                get(1) shouldBeEqualTo combinedMessageContactEntity2
            }
        }
    }

    @Test
    fun `given dao operation fails, when when getting latest unread message by conversationId, then returns failure`() {
        coEvery { messageDao.latestUnreadMessagesByConversationId(any(), any()) } throws SQLException()

        val result = runBlocking { messageLocalDataSource.latestUnreadMessagesByConversationId(any(), any()) }

        result shouldFail { }
        coVerify(exactly = 1) { messageDao.latestUnreadMessagesByConversationId(any(), any()) }
    }

    @Test
    fun `given dao operation returns messages, when getting latest unread message by conversationId, then returns messages`() {
        val combinedMessageContactEntity1 = mockk<CombinedMessageContactEntity>()
        val combinedMessageContactEntity2 = mockk<CombinedMessageContactEntity>()
        val messages = listOf(combinedMessageContactEntity1, combinedMessageContactEntity2)
        coEvery { messageDao.latestUnreadMessagesByConversationId(any(), any()) } returns messages

        val result = runBlocking { messageLocalDataSource.latestUnreadMessagesByConversationId(any(), any()) }

        result shouldSucceed {
            it.size shouldBeEqualTo 2
        }
    }

    @Test
    fun `given dao returns a message, when getting message by id, then return said message`() {
        val messageId = "i123"
        val messageEntity = MessageEntity(
            messageId, "conv", "send", "type",
            "content", "state", "time", false
        )
        coEvery { messageDao.messageById(messageId) } returns messageEntity

        val result = runBlocking { messageLocalDataSource.messageById(messageId) }

        result shouldSucceed {
            it shouldBeEqualTo messageEntity
        }
    }

    @Test
    fun `given dao returns a message, when getting message by id, then the dao should be called once`() {
        val messageId = "i123"
        val messageEntity = MessageEntity(
            messageId, "conv", "send", "type",
            "content", "state", "time", false
        )
        coEvery { messageDao.messageById(messageId) } returns messageEntity

        runBlocking { messageLocalDataSource.messageById(messageId) }

        coVerify(exactly = 1) { messageDao.messageById(messageId) }
    }

    @Test
    fun `given dao returns a failure, when getting message by id, then forward the failure`() {
        val messageId = "i123"
        coEvery { messageDao.messageById(messageId) } throws SQLException()

        val result = runBlocking { messageLocalDataSource.messageById(messageId) }

        result shouldFail {}
    }

}
