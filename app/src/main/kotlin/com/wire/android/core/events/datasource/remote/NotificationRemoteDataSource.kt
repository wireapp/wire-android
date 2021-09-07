package com.wire.android.core.events.datasource.remote

import com.tinder.scarlet.WebSocket
import com.wire.android.core.events.Event
import com.wire.android.core.events.WebSocketConfig
import com.wire.android.core.events.mapper.EventMapper
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class NotificationRemoteDataSource(
    private val webSocketServiceProvider: WebSocketServiceProvider,
    private val webSocketConfig: WebSocketConfig,
    private val notificationApi: NotificationApi,
    private val eventMapper: EventMapper,
    override val networkHandler: NetworkHandler
) : ApiService() {

    fun receiveEvents(clientId: String): Flow<List<Event>?> =
        webSocketServiceForClient(clientId).receiveEvent().map {
            it.payload?.let { payloads ->
                payloads.map { payload -> eventMapper.eventFromPayload(payload, it.id) }
            }
        }

    suspend fun notificationsFlow(clientId: String, notificationId: String): Flow<List<Event>> =
        webSocketServiceForClient(clientId)
            .observeWebSocketEvent().filter {
                it is WebSocket.Event.OnConnectionOpened<*>
            }.map { allNotifications(clientId, notificationId) }

    private fun webSocketServiceForClient(clientId: String) =
        webSocketServiceProvider.provideWebSocketService(webSocketConfig.urlForClient(clientId))

    private suspend fun notificationsByBatch(size: Int, client: String, since: String): Either<Failure, NotificationPageResponse> =
        request { notificationApi.notificationsByBatch(size, client, since) }

    private suspend fun allNotifications(clientId: String, notificationId: String): List<Event> =
        suspending {
            val notifications = mutableListOf<Event>()
            var hasMore = true
            while (hasMore) {
                notificationsByBatch(PAGE_SIZE, clientId, notificationId).fold({
                    hasMore = false
                }) { notificationPageResponse ->
                    hasMore = notificationPageResponse.hasMore
                    notificationPageResponse.notifications.forEach { notificationResponse ->
                        notificationResponse.payload?.let { payloads ->
                            payloads.forEach { payload ->
                                val event = eventMapper.eventFromPayload(payload, notificationResponse.id)
                                notifications.add(event)
                            }
                        }
                    }
                }
            }
            return@suspending notifications
        }

    suspend fun lastNotification(client: String): Either<Failure, NotificationResponse> = request {
        notificationApi.lastNotification(client)
    }

    companion object {
        const val PAGE_SIZE = 500
    }
}
