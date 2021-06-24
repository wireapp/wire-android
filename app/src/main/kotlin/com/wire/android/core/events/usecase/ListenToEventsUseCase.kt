package com.wire.android.core.events.usecase

import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.handler.EventsHandler
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import kotlinx.coroutines.flow.collect

class ListenToEventsUseCase(
    private val eventRepository: EventRepository,
    private val eventHandler: EventsHandler<Event>
) : UseCase<Unit, Unit> {
    override suspend fun run(params: Unit): Either<Failure, Unit> {
        eventRepository.events().collect {
            if (it is Event.Conversation.Message)
                eventHandler.subscribe(it)
        }
        return Either.Right(Unit)
    }
}
