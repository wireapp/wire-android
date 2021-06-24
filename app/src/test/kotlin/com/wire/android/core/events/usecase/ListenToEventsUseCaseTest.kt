package com.wire.android.core.events.usecase

import com.wire.android.UnitTest
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.handler.EventsHandler
import io.mockk.impl.annotations.MockK
import org.junit.Before

//TODO
class ListenToEventsUseCaseTest : UnitTest() {

    @MockK
    private lateinit var eventRepository: EventRepository

    @MockK
    private lateinit var eventsHandler: EventsHandler<Event>

    private lateinit var listenToEventsUseCase: ListenToEventsUseCase

    @Before
    fun setup() {
        listenToEventsUseCase = ListenToEventsUseCase(eventRepository, eventsHandler)
    }
}
