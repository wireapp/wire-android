package com.wire.android.feature.conversation.content.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.Sent
import com.wire.android.feature.conversation.content.Text
import com.wire.android.feature.conversation.content.datasources.local.MessageEntity
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class MessageMapperTest : UnitTest() {

    @MockK
    private lateinit var messageTypeMapper: MessageTypeMapper

    @MockK
    private lateinit var messageStateMapper: MessageStateMapper

    private lateinit var messageMapper: MessageMapper

    @Before
    fun setUp() {
        messageMapper = MessageMapper(messageTypeMapper, messageStateMapper)
    }

    @Test
    fun `given fromEntityToMessage is called, then maps the MessageEntity and returns a Message`() {

        every { messageTypeMapper.fromStringValue(TEST_MESSAGE_TYPE) } returns Text
        every { messageStateMapper.fromStringValue(TEST_MESSAGE_STATE) } returns Sent
        val messageEntity = MessageEntity(
            id = TEST_MESSAGE_ID,
            conversationId = TEST_CONVERSATION_ID,
            userId = TEST_USER_ID,
            type = TEST_MESSAGE_TYPE,
            content = TEST_MESSAGE_CONTENT,
            state = TEST_MESSAGE_STATE,
            time = TEST_MESSAGE_TIME
        )

        val result = messageMapper.fromEntityToMessage(messageEntity)

        result.let {
            it shouldBeInstanceOf Message::class
            it.id shouldBeEqualTo TEST_MESSAGE_ID
            it.conversationId shouldBeEqualTo TEST_CONVERSATION_ID
            it.userId shouldBeEqualTo TEST_USER_ID
            it.type shouldBeEqualTo Text
            it.content shouldBeEqualTo TEST_MESSAGE_CONTENT
            it.state shouldBeEqualTo Sent
            it.time shouldBeEqualTo TEST_MESSAGE_TIME
        }

    }

    @Test
    fun `given fromMessageToEntity is called, then maps the Message and returns a MessageEntity`() {
        every { messageTypeMapper.fromValueToString(Text) } returns TEST_MESSAGE_TYPE
        every { messageStateMapper.fromValueToString(Sent) } returns TEST_MESSAGE_STATE
        val message = Message(
            id = TEST_MESSAGE_ID,
            conversationId = TEST_CONVERSATION_ID,
            userId = TEST_USER_ID,
            clientId = null,
            type = Text,
            content = TEST_MESSAGE_CONTENT,
            state = Sent,
            time = TEST_MESSAGE_TIME
        )

        val result = messageMapper.fromMessageToEntity(message)

        result.let {
            it shouldBeInstanceOf MessageEntity::class
            it.id shouldBeEqualTo TEST_MESSAGE_ID
            it.conversationId shouldBeEqualTo TEST_CONVERSATION_ID
            it.userId shouldBeEqualTo TEST_USER_ID
            it.type shouldBeEqualTo TEST_MESSAGE_TYPE
            it.content shouldBeEqualTo TEST_MESSAGE_CONTENT
            it.state shouldBeEqualTo TEST_MESSAGE_STATE
            it.time shouldBeEqualTo TEST_MESSAGE_TIME
        }
    }

    companion object {
        private const val TEST_MESSAGE_ID = "126454245456"
        private const val TEST_CONVERSATION_ID = "78897845445655"
        private const val TEST_USER_ID = "05568897845445655"
        private const val TEST_MESSAGE_TYPE = "text"
        private const val TEST_MESSAGE_CONTENT = "Hello!"
        private const val TEST_MESSAGE_STATE = "sent"
        private const val TEST_MESSAGE_TIME = "1624990688"
    }
}
