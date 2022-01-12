package com.wire.android.core.events.usecase

import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.handler.EventsHandler
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.flow.collect

class ListenToEventsUseCase(
    private val eventRepository: EventRepository,
    private val messageEventHandler: EventsHandler<Event.Conversation.MessageEvent>
) : UseCase<Unit, Unit> {
    override suspend fun run(params: Unit): Either<Failure, Unit> {
        suspending {
            eventRepository.events().collect {
                it.map { event ->
                    if (event is Event.Conversation.MessageEvent)
                        messageEventHandler.subscribe(event)
                }
            }
        }
        return Either.Right(Unit)
    }
}
