package com.wire.android.core.events.usecase

import com.wire.android.UnitTest
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.handler.EventsHandler
import com.wire.android.core.functional.Either
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ListenToEventsUseCaseTest : UnitTest() {

    @MockK
    private lateinit var eventRepository: EventRepository

    @MockK
    private lateinit var messageEventsHandler: EventsHandler<Event.Conversation.MessageEvent>

    private lateinit var listenToEventsUseCase: ListenToEventsUseCase

    @Before
    fun setup() {
        listenToEventsUseCase = ListenToEventsUseCase(eventRepository, messageEventsHandler)
    }

    @Test
    fun `given eventRepository emits events, when event is Message, then subscribe to messageEventHandler`() {
        val event = mockk<Event.Conversation.MessageEvent>()
        every { eventRepository.events() } returns flowOf(Either.Right(event))

        runBlocking {
            listenToEventsUseCase.run(Unit)

            coVerify(exactly = 1) { messageEventsHandler.subscribe(event) }
        }
    }

    @Test
    fun `given eventRepository emits events, when event is not Message, then do not subscribe to messageEventHandler`() {
        val event = mockk<Event.Unknown>()
        every { eventRepository.events() } returns flowOf(Either.Right(event))

        runBlocking {
            listenToEventsUseCase.run(Unit)

            coVerify(exactly = 0) { messageEventsHandler.subscribe(any()) }
        }
    }
}
