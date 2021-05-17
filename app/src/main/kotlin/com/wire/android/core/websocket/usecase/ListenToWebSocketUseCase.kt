package com.wire.android.core.websocket.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.core.websocket.data.EventRepository
import com.wire.android.core.websocket.data.Message
import kotlinx.coroutines.channels.Channel

class ListenToWebSocketUseCase(private val repository: EventRepository) : UseCase<Channel<Message>, Unit> {
    override suspend fun run(params: Unit): Either<Failure, Channel<Message>> = Either.Right(repository.startSocket())
}
