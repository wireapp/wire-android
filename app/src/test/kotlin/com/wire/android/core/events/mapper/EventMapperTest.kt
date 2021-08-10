package com.wire.android.core.events.mapper

import com.wire.android.UnitTest
import com.wire.android.core.events.Event
import com.wire.android.core.events.datasource.remote.Data
import com.wire.android.core.events.datasource.remote.Payload
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class EventMapperTest : UnitTest() {

    private lateinit var eventMapper: EventMapper

    @Before
    fun setUp() {
        eventMapper = EventMapper()
    }

    @Test
    fun `given eventFromPayload is called and payload type is new message, when payload data is null, then return unknown event`() {
        val payload = mockk<Payload>().also {
            every { it.type } returns NEW_MESSAGE_TYPE
            every { it.data } returns null
        }

        val result = eventMapper.eventFromPayload(payload, EVENT_ID)

        result shouldBeInstanceOf Event.Unknown::class
    }

    @Test
    fun `given eventFromPayload is called, when payload type is not new message, then return unknown event`() {
        val payload = mockk<Payload>().also {
            every { it.type } returns "nothing"
        }

        val result = eventMapper.eventFromPayload(payload, EVENT_ID)

        result shouldBeInstanceOf Event.Unknown::class
    }

    @Test
    fun `given eventFromPayload is called and payload type is new message, when payload data is not null, then return a valid message event`() {
        val data = mockk<Data>().also {
            every { it.sender } returns SENDER
            every { it.text } returns TEXT
        }
        val payload = mockk<Payload>().also {
            every { it.conversation } returns CONVERSATION_ID
            every { it.from } returns FROM
            every { it.time } returns TIME
            every { it.data } returns data
            every { it.type } returns NEW_MESSAGE_TYPE
        }

        val result = eventMapper.eventFromPayload(payload, EVENT_ID)

        result shouldBeInstanceOf Event.Conversation.MessageEvent::class
        with(result as Event.Conversation.MessageEvent) {
            id shouldBeEqualTo EVENT_ID
            conversationId shouldBeEqualTo CONVERSATION_ID
            senderClientId shouldBeEqualTo SENDER
            senderUserId shouldBeEqualTo FROM
            content shouldBeEqualTo TEXT
            time shouldBeEqualTo TIME
        }
    }

    companion object {
        private const val EVENT_ID = "event-id"
        private const val CONVERSATION_ID = "conversation-id"
        private const val TIME = "time"
        private const val FROM = "from"
        private const val TEXT = "text"
        private const val SENDER = "sender"
        private const val NEW_MESSAGE_TYPE = "conversation.otr-message-add"
    }

}