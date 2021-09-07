package com.wire.android.core.events.datasource

import com.tinder.scarlet.Lifecycle
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.WebSocketConfig
import com.wire.android.core.events.datasource.remote.WebSocketService
import com.wire.android.core.events.datasource.remote.WebSocketServiceProvider
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient

class EventDataSource(private val okHttpClient: OkHttpClient,
                      private val lifecycle: Lifecycle,
                      private val sessionRepository: SessionRepository,
                      private val webSocketConfig: WebSocketConfig,
                      private val webSocketServiceProvider: WebSocketServiceProvider) : EventRepository {
    override fun events(): Flow<Event> = flow {
        sessionRepository.currentClientId().isRight
        webSocketConfig.urlForClient()
        webSocketServiceProvider.provideWebSocketService(okHttpClient, lifecycle, webSocketConfig.)
        webSocketService.receiveEvent().collect {
            it.payload?.let { payloads ->
                for (payload in payloads)
                    if (payload.type == NEW_MESSAGE_TYPE && payload.data != null)
                        emit(
                            Event.Conversation.MessageEvent(
                                it.id,
                                payload.conversation,
                                payload.data.sender,
                                payload.from,
                                payload.data.text,
                                payload.time
                            )
                        )
            }
        }
    }

    companion object {
        const val NEW_MESSAGE_TYPE = "conversation.otr-message-add"
    }
}
