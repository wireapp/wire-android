package com.wire.android.feature.conversation.content.datasources.local

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.content.Sent
import com.wire.android.feature.conversation.content.mapper.MessageStateMapper
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
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.any
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class MessageLocalDataSourceTest : UnitTest() {

    private lateinit var messageLocalDataSource: MessageLocalDataSource

    @MockK
    private lateinit var messageDao: MessageDao

    @MockK
    private lateinit var messageStateMapper: MessageStateMapper

    @Before
    fun setUp() {
        messageLocalDataSource = MessageLocalDataSource(messageDao, messageStateMapper)
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
        coEvery { messageDao.messageById(TEST_MESSAGE_ID) } returns TEST_MESSAGE_ENTITY

        val result = runBlocking { messageLocalDataSource.messageById(TEST_MESSAGE_ID) }

        result shouldSucceed {
            it shouldBeEqualTo TEST_MESSAGE_ENTITY
        }
    }

    @Test
    fun `given dao returns a message, when getting message by id, then the dao should be called once`() {
        coEvery { messageDao.messageById(TEST_MESSAGE_ID) } returns TEST_MESSAGE_ENTITY

        runBlocking { messageLocalDataSource.messageById(TEST_MESSAGE_ID) }

        coVerify(exactly = 1) { messageDao.messageById(TEST_MESSAGE_ID) }
    }

    @Test
    fun `given dao returns a failure, when getting message by id, then forward the failure`() {
        coEvery { messageDao.messageById(TEST_MESSAGE_ID) } throws SQLException()

        val result = runBlocking { messageLocalDataSource.messageById(TEST_MESSAGE_ID) }

        result shouldFail {}
    }

    @Test
    fun `given a messageId, when marking message as sent, then the correct ID should be passed to the dao`() {
        coEvery { messageDao.messageById(any()) } returns TEST_MESSAGE_ENTITY

        runBlockingTest { messageLocalDataSource.markMessageAsSent(TEST_MESSAGE_ID) }

        coVerify(exactly = 1) { messageDao.messageById(TEST_MESSAGE_ID) }
    }

    @Test
    fun `given the dao returns successfully, when marking message as sent, then the fetched entity should be updated with mapped state`() {
        val sentState = "sentState"
        coEvery { messageDao.messageById(any()) } returns TEST_MESSAGE_ENTITY
        coEvery { messageStateMapper.fromValueToString(Sent) } returns sentState

        runBlockingTest { messageLocalDataSource.markMessageAsSent(TEST_MESSAGE_ID) }

        coVerify(exactly = 1) { messageDao.insert(TEST_MESSAGE_ENTITY.copy(state = sentState)) }
    }

    @Test
    fun `given dao fails during fetching, when marking message as sent, then forward failure`() {
        coEvery { messageDao.messageById(any()) } throws SQLException()

        val result = runBlocking { messageLocalDataSource.markMessageAsSent(TEST_MESSAGE_ID) }

        result shouldFail {}
    }

    @Test
    fun `given dao fails during update, when marking message as sent, then forward failure`() {
        coEvery { messageDao.messageById(any()) } returns TEST_MESSAGE_ENTITY
        coEvery { messageDao.insert(any()) } throws SQLException()

        val result = runBlocking { messageLocalDataSource.markMessageAsSent(TEST_MESSAGE_ID) }

        result shouldFail {}
    }

    companion object {
        private const val TEST_MESSAGE_ID = "i312"
        private val TEST_MESSAGE_ENTITY = MessageEntity(
            TEST_MESSAGE_ID, "conv", "send", "type",
            "content", "state", "time", false
        )
    }
}
