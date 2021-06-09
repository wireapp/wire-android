package com.wire.android.core.websocket.usecase

import com.wire.android.core.usecase.ObservableUseCase
import com.wire.android.core.websocket.data.EventDataSource
import com.wire.android.core.websocket.data.Message
import kotlinx.coroutines.flow.Flow

class ListenToWebSocketUseCase(private val repository: EventDataSource) : ObservableUseCase<Message, Unit> {
    override suspend fun run(params: Unit): Flow<Message> = repository.startSocket()
}
