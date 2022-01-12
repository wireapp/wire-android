package com.wire.android.feature.conversation.content.mapper

import com.wire.android.UnitTest
import com.wire.android.core.crypto.model.CryptoClientId
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.date.DateStringMapper
import com.wire.android.core.events.Event
import com.wire.android.feature.conversation.content.Content
import com.wire.android.feature.conversation.content.EncryptedMessageEnvelope
import com.wire.android.feature.conversation.content.Message
import com.wire.android.feature.conversation.content.Sent
import com.wire.android.feature.conversation.content.datasources.local.MessageEntity
import com.wire.android.shared.user.QualifiedId
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test
import java.time.OffsetDateTime

class MessageMapperTest : UnitTest() {

    @MockK
    private lateinit var messageContentMapper: MessageContentMapper

    @MockK
    private lateinit var messageStateMapper: MessageStateMapper

    @MockK
    private lateinit var dateStringMapper: DateStringMapper

    private lateinit var messageMapper: MessageMapper

    @Before
    fun setUp() {
        messageMapper = MessageMapper(messageContentMapper, messageStateMapper, dateStringMapper)
    }

    @Test
    fun `given fromEntityToMessage is called, then maps the MessageEntity and returns a Message`() {
        val expectedTimeOffset: OffsetDateTime = mockk()

        every { messageContentMapper.fromStringToContent(TEST_MESSAGE_TYPE, TEST_MESSAGE_CONTENT_VALUE) } returns TEST_MESSAGE_CONTENT
        every { messageStateMapper.fromStringValue(TEST_MESSAGE_STATE) } returns Sent
        every { dateStringMapper.fromStringToOffsetDateTime(TEST_MESSAGE_TIME) } returns expectedTimeOffset

        val messageEntity = MessageEntity(
            id = TEST_MESSAGE_ID,
            conversationId = TEST_CONVERSATION_ID,
            senderUserId = TEST_SENDER_USER_ID,
            type = TEST_MESSAGE_TYPE,
            content = TEST_MESSAGE_CONTENT_VALUE,
            state = TEST_MESSAGE_STATE,
            time = TEST_MESSAGE_TIME,
            isRead = TEST_MESSAGE_IS_READ
        )

        val result = messageMapper.fromEntityToMessage(messageEntity)

        result.let {
            it shouldBeInstanceOf Message::class
            it.id shouldBeEqualTo TEST_MESSAGE_ID
            it.conversationId shouldBeEqualTo TEST_CONVERSATION_ID
            it.senderUserId shouldBeEqualTo TEST_SENDER_USER_ID
            it.content shouldBeEqualTo TEST_MESSAGE_CONTENT
            it.isRead shouldBeEqualTo TEST_MESSAGE_IS_READ
            it.state shouldBeEqualTo Sent
            it.time shouldBeEqualTo expectedTimeOffset
        }
        verify(exactly = 1) { messageContentMapper.fromStringToContent(TEST_MESSAGE_TYPE, TEST_MESSAGE_CONTENT_VALUE) }
        verify(exactly = 1) { messageStateMapper.fromStringValue(TEST_MESSAGE_STATE) }
        verify(exactly = 1) { dateStringMapper.fromStringToOffsetDateTime(TEST_MESSAGE_TIME) }

    }

    @Test
    fun `given fromMessageToEntity is called, then maps the Message and returns a MessageEntity`() {
        val timeOffset: OffsetDateTime = mockk()

        every { messageContentMapper.fromContentToString(TEST_MESSAGE_CONTENT) } returns TEST_MESSAGE_CONTENT_VALUE
        every { messageContentMapper.fromContentToStringType(TEST_MESSAGE_CONTENT) } returns TEST_MESSAGE_TYPE
        every { messageStateMapper.fromValueToString(Sent) } returns TEST_MESSAGE_STATE
        every { dateStringMapper.fromOffsetDateTimeToString(timeOffset) } returns TEST_MESSAGE_TIME
        val message = Message(
            id = TEST_MESSAGE_ID,
            conversationId = TEST_CONVERSATION_ID,
            senderUserId = TEST_SENDER_USER_ID,
            content = TEST_MESSAGE_CONTENT,
            isRead = TEST_MESSAGE_IS_READ,
            clientId = null,
            state = Sent,
            time = timeOffset
        )

        val result = messageMapper.fromMessageToEntity(message)

        result.let {
            it shouldBeInstanceOf MessageEntity::class
            it.id shouldBeEqualTo TEST_MESSAGE_ID
            it.conversationId shouldBeEqualTo TEST_CONVERSATION_ID
            it.senderUserId shouldBeEqualTo TEST_SENDER_USER_ID
            it.type shouldBeEqualTo TEST_MESSAGE_TYPE
            it.content shouldBeEqualTo TEST_MESSAGE_CONTENT_VALUE
            it.state shouldBeEqualTo TEST_MESSAGE_STATE
            it.time shouldBeEqualTo TEST_MESSAGE_TIME
            it.isRead shouldBeEqualTo TEST_MESSAGE_IS_READ
        }

        verify(exactly = 1) { messageContentMapper.fromContentToString(TEST_MESSAGE_CONTENT) }
        verify(exactly = 1) { messageContentMapper.fromContentToStringType(TEST_MESSAGE_CONTENT) }
        verify(exactly = 1) { messageStateMapper.fromValueToString(Sent) }
        verify(exactly = 1) { dateStringMapper.fromOffsetDateTimeToString(timeOffset) }
    }

    @Test
    fun `given fromMessageEventToEncryptedEnvelope is called, then maps the MessageEvent and returns an EncryptedEnvelope`() {
        val expectedTimeOffset: OffsetDateTime = mockk()
        every { dateStringMapper.fromStringToOffsetDateTime(TEST_MESSAGE_TIME) } returns expectedTimeOffset

        val messageEvent = Event.Conversation.MessageEvent(
            id = TEST_MESSAGE_ID,
            conversationId = TEST_CONVERSATION_ID,
            senderClientId = TEST_SENDER_ID,
            senderUserId = TEST_SENDER_USER_ID,
            content = TEST_MESSAGE_CONTENT_VALUE,
            time = TEST_MESSAGE_TIME
        )

        val result = messageMapper.fromMessageEventToEncryptedMessageEnvelope(messageEvent)

        result.let {
            it shouldBeInstanceOf EncryptedMessageEnvelope::class
            it.id shouldBeEqualTo TEST_MESSAGE_ID
            it.conversationId shouldBeEqualTo TEST_CONVERSATION_ID
            it.senderUserId shouldBeEqualTo TEST_SENDER_USER_ID
            it.content shouldBeEqualTo TEST_MESSAGE_CONTENT_VALUE
            it.time shouldBeEqualTo expectedTimeOffset
        }
    }

    @Test
    fun `given an EncryptedMessageEnvelope, when mapping to a CryptoSession, then return a CryptoSessionId`() {
        val message = mockk<EncryptedMessageEnvelope>().also {
            every { it.senderUserId } returns TEST_SENDER_USER_ID
            every { it.clientId } returns TEST_SENDER_ID
        }
        val expected = "${TEST_SENDER_USER_ID}_${TEST_SENDER_ID}"

        val result = messageMapper.cryptoSessionFromEncryptedEnvelope(message)

        result.let {
            it shouldBeInstanceOf CryptoSessionId::class
            it.userId shouldBeInstanceOf QualifiedId::class
            it.cryptoClientId shouldBeInstanceOf CryptoClientId::class
            it.value shouldBeEqualTo expected
        }
    }

    companion object {
        private const val TEST_MESSAGE_ID = "message-id"
        private const val TEST_CONVERSATION_ID = "conversation-id"
        private const val TEST_SENDER_ID = "sender-id"
        private const val TEST_SENDER_USER_ID = "sender-user-id"
        private const val TEST_MESSAGE_TYPE = "text"
        private const val TEST_MESSAGE_CONTENT_VALUE = "Hello!"
        private val TEST_MESSAGE_CONTENT = Content.Text(TEST_MESSAGE_CONTENT_VALUE)
        private const val TEST_MESSAGE_STATE = "sent"
        private const val TEST_MESSAGE_TIME = "2019-12-12T21:21:00Z+03:00"
        private const val TEST_MESSAGE_IS_READ = true
    }
}
