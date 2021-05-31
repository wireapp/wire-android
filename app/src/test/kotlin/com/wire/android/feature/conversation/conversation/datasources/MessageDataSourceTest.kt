package com.wire.android.feature.conversation.conversation.datasources

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.conversation.Message
import com.wire.android.feature.conversation.conversation.datasources.local.MessageEntity
import com.wire.android.feature.conversation.conversation.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.conversation.mapper.MessageMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
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

    @Before
    fun setUp() {
        messageDataSource = MessageDataSource(messageLocalDataSource, messageMapper)
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
