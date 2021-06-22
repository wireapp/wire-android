package com.wire.android.core.events.usecase

import com.wire.android.core.events.Event
import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.core.events.EventRepository
import kotlinx.coroutines.flow.Flow

class ListenToEventsUseCase(private val eventRepository: EventRepository) : ObservableUseCase<Event, Unit> {
    override suspend fun run(params: Unit): Flow<Event> = eventRepository.events()
}
