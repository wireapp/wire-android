package com.wire.android.core.events.datasource

import com.wire.android.UnitTest
import com.wire.android.core.events.Event
import com.wire.android.core.events.datasource.remote.Data
import com.wire.android.core.events.datasource.remote.EventResponse
import com.wire.android.core.events.datasource.remote.Payload
import com.wire.android.core.events.datasource.remote.WebSocketService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class EventDataSourceTest : UnitTest() {

    @MockK
    private lateinit var webSocketService: WebSocketService

    private lateinit var eventDataSource: EventDataSource

    @Before
    fun setUp() {
        eventDataSource = EventDataSource(webSocketService)
    }

    @Test
    fun `given websocket emits events, when payload is null, then do not emit data`() {
        val eventResponse = mockk<EventResponse>().also {
            every { it.payload } returns null
        }
        every { webSocketService.receiveEvent() } returns flowOf(eventResponse)

        runBlocking {
            val result = eventDataSource.events()

            result.count() shouldBeEqualTo 0
            verify(exactly = 1) { webSocketService.receiveEvent() }
        }
    }

    @Test
    fun `given websocket emits events, when payload is empty, then do not emit data`() {
        val eventResponse = mockk<EventResponse>().also {
            every { it.payload } returns listOf()
        }
        every { webSocketService.receiveEvent() } returns flowOf(eventResponse)

        runBlocking {
            val result = eventDataSource.events()

            result.count() shouldBeEqualTo 0
            verify(exactly = 1) { webSocketService.receiveEvent() }
        }
    }

    @Test
    fun `given websocket emits events, when payload data is null, then do not emit data`() {
        val payload = mockk<Payload>().also {
            every { it.data } returns null
            every { it.type } returns Event.Conversation.NEW_MESSAGE_TYPE
        }
        val eventResponse = mockk<EventResponse>().also {
            every { it.payload } returns listOf(payload)
        }
        every { webSocketService.receiveEvent() } returns flowOf(eventResponse)

        runBlocking {
            val result = eventDataSource.events()

            result.count() shouldBeEqualTo 0
            verify(exactly = 1) { webSocketService.receiveEvent() }
        }
    }

    @Test
    fun `given websocket emits events, when payload type is not a message-add, then do not emit data`() {
        val data = mockk<Data>()
        val payload = mockk<Payload>().also {
            every { it.data } returns data
            every { it.type } returns ""
        }

        val eventResponse = mockk<EventResponse>().also {
            every { it.payload } returns listOf(payload)
        }
        every { webSocketService.receiveEvent() } returns flowOf(eventResponse)

        runBlocking {
            val result = eventDataSource.events()

            result.count() shouldBeEqualTo 0
            verify(exactly = 1) { webSocketService.receiveEvent() }
        }
    }

    @Test
    fun `given websocket emits events and payload type is message-add, when payload data is not null, then emit Message event`() {
        val data = mockk<Data>().also {
            every { it.sender } returns SENDER
            every { it.text } returns TEXT
        }
        val payload = mockk<Payload>().also {
            every { it.data } returns data
            every { it.type } returns Event.Conversation.NEW_MESSAGE_TYPE
            every { it.conversation } returns CONVERSATION_ID
        }
        val eventResponse = mockk<EventResponse>().also {
            every { it.id } returns NOTIFICATION_ID
            every { it.payload } returns listOf(payload)
        }
        every { webSocketService.receiveEvent() } returns flowOf(eventResponse)

        runBlocking {
            val result = eventDataSource.events()

            val expected = Event.Conversation.Message(eventResponse.id, payload.conversation, data.sender, data.text)
            with(result) {
                count() shouldBeEqualTo 1
                first() shouldBeEqualTo expected
            }
        }
    }

    @Test
    fun `given websocket emits events, when event have two payloads, then emit two Message events`() {
        val data = mockk<Data>().also {
            every { it.sender } returns SENDER
            every { it.text } returns TEXT
        }
        val payload = mockk<Payload>().also {
            every { it.data } returns data
            every { it.type } returns Event.Conversation.NEW_MESSAGE_TYPE
            every { it.conversation } returns CONVERSATION_ID
        }
        val eventResponse = mockk<EventResponse>().also {
            every { it.id } returns NOTIFICATION_ID
            every { it.payload } returns listOf(payload, payload)
        }
        every { webSocketService.receiveEvent() } returns flowOf(eventResponse)

        runBlocking {
            val result = eventDataSource.events()
            result.count() shouldBeEqualTo 2
            result.onEach { it shouldBeInstanceOf Event.Conversation.Message::class }
        }
    }

    companion object {
        private const val CONVERSATION_ID = "213sdd4564787"
        private const val NOTIFICATION_ID = "sdfsdfef85544"
        private const val SENDER = "sdfsdfef85544"
        private const val TEXT = "qsdferdsfsdfsd4545sd45f45sdf4455s4df454"
    }
}

