package com.wire.android.core.events.datasource.remote

import com.tinder.scarlet.WebSocket
import com.wire.android.core.events.mapper.EventMapper
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class NotificationRemoteDataSource(
    private val webSocketService: WebSocketService,
    private val notificationApi: NotificationApi,
    private val eventMapper: EventMapper,
    override val networkHandler: NetworkHandler
) : ApiService() {

    suspend fun receiveEvents() = flow {
        webSocketService.receiveEvent().collect {
            it.payload?.let { payloads ->
                payloads.forEach { payload ->
                    val event = eventMapper.eventFromPayload(payload, it.id)
                    emit(event)
                }
            }
        }
    }

    suspend fun observeWebSocketEvents(clientId: String, notificationId: String) = flow {
        suspending {
            webSocketService.observeWebSocketEvent().collect {
                if (it is WebSocket.Event.OnConnectionOpened<*>) {
                    var hasMore = true
                    while (hasMore) {
                        notificationsByBatch(PAGE_SIZE, clientId, notificationId).onSuccess { notificationPageResponse ->
                            hasMore = notificationPageResponse.hasMore
                            notificationPageResponse.notifications.forEach { notificationResponse ->
                                notificationResponse.payload?.let { payloads ->
                                    payloads.forEach { payload ->
                                        val event = eventMapper.eventFromPayload(payload, notificationResponse.id)
                                        emit(event)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun lastNotification(client: String): Either<Failure, NotificationResponse> = request {
        notificationApi.lastNotification(client)
    }

    private suspend fun notificationsByBatch(
        size: Int,
        client: String,
        since: String
    ): Either<Failure, NotificationPageResponse> =
        request { notificationApi.notificationsByBatch(size, client, since) }

    companion object {
        const val PAGE_SIZE = 500
    }
}
