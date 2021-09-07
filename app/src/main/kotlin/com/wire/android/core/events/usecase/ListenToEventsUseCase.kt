package com.wire.android.core.events.usecase

import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.handler.EventsHandler
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.flow.collect

class ListenToEventsUseCase(
    private val eventRepository: EventRepository,
    private val messageEventHandler: EventsHandler<Event.Conversation.MessageEvent>
) : UseCase<Unit, Unit> {
    override suspend fun run(params: Unit): Either<Failure, Unit> {
        eventRepository.events().collect {
            if (it is Event.Conversation.MessageEvent)
                messageEventHandler.subscribe(it)
        }
        return Either.Right(Unit)
    }
}
