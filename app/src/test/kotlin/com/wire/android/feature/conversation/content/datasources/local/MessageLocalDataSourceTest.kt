package com.wire.android.feature.conversation.content.datasources.local

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.sql.SQLException

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
    fun `given messagesByConversationId is called, when no message exists, then don't emits messages`() {
        val conversationId = "conversationId"
        coEvery { messageDao.messagesByConversationId(conversationId) } returns flowOf(listOf())

        runBlocking {
            val result = messageLocalDataSource.messagesByConversationId(conversationId)

            result.first().size shouldBeEqualTo 0
        }
    }

    @Test
    fun `given messagesByConversationId is called, when when dao operation is successful, then emits a list of messages`() {
        val conversationId = "conversationId"
        val messageEntity1 = mockk<MessageEntity>()
        val messageEntity2 = mockk<MessageEntity>()

        coEvery { messageDao.messagesByConversationId(conversationId) } returns flowOf(listOf(messageEntity1, messageEntity2))

        runBlocking {
            val result = messageLocalDataSource.messagesByConversationId(conversationId)

            with(result.first()){
                size shouldBeEqualTo 2
                get(0) shouldBeEqualTo messageEntity1
                get(1) shouldBeEqualTo messageEntity2
            }
        }
    }
}
