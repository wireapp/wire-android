package com.wire.android.core.events.usecase

import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.remote.EventResponse
import kotlinx.coroutines.flow.Flow

class ListenToEventsUseCase(private val eventRepository: EventRepository) : ObservableUseCase<EventResponse, Unit> {
    override suspend fun run(params: Unit): Flow<EventResponse> = eventRepository.events()
}
