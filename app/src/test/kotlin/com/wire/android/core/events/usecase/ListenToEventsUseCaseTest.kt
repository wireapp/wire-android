package com.wire.android.core.events.usecase

import com.wire.android.UnitTest
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ListenToEventsUseCaseTest : UnitTest() {

    @MockK
    private lateinit var eventRepository: EventRepository

    private lateinit var listenToEventsUseCase: ListenToEventsUseCase

    @Before
    fun setup() {
        listenToEventsUseCase = ListenToEventsUseCase(eventRepository)
    }

    @Test
    fun `given run is called, when eventRepository emits items, then propagates items`() {
        val items = mockk<Event>()
        coEvery { eventRepository.events() } returns flowOf(items)

        runBlocking {
            val result = listenToEventsUseCase.run(Unit)
            result.first() shouldBeEqualTo items
        }
    }
}
