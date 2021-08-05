package com.wire.android.core.events.datasource

import com.tinder.scarlet.WebSocket
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.events.datasource.remote.WebSocketService
import com.wire.android.core.events.mapper.EventMapper
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.map
import com.wire.android.core.functional.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class EventDataSource(
    private val webSocketService: WebSocketService,
    private val externalScope: CoroutineScope,
    private val notificationLocalDataSource: NotificationLocalDataSource,
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val eventMapper: EventMapper,
    private val clientId: String
) : EventRepository {
    override fun events(): Flow<Event> = callbackFlow {

        externalScope.launch {
            webSocketService.observeWebSocketEvent().collect {
                if (it is WebSocket.Event.OnConnectionOpened<*>) {
                    val notificationId = lastNotificationId(clientId)
                    var hasMore = true
                    while (hasMore) {
                        notificationRemoteDataSource.notificationsByBatch(PAGE_SIZE, clientId, notificationId)
                            .onSuccess { notificationPageResponse ->
                                hasMore = notificationPageResponse.hasMore
                                notificationPageResponse.notifications.forEach { notificationResponse ->
                                    notificationResponse.payload?.let { payloads ->
                                        payloads.forEach { payload ->
                                            val event = eventMapper.eventFromPayload(payload, notificationResponse.id)
                                            trySendBlocking(event)
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }

        externalScope.launch {
            webSocketService.receiveEvent().collect {
                it.payload?.let { payloads ->
                    payloads.forEach { payload ->
                        val event = eventMapper.eventFromPayload(payload, it.id)
                        trySendBlocking(event)
                    }
                }
            }
        }

        awaitClose { }
    }

    //TODO this function should be moved to be called in full state sync
    private suspend fun lastNotificationId(clientId: String) : String {
        notificationLocalDataSource.lastNotificationId()?.let {
            return it
        } ?: run {
            notificationRemoteDataSource.lastNotification(clientId).map { notificationResponse ->
                    notificationLocalDataSource.saveLastNotificationId(notificationResponse.id)
                    return@map notificationResponse.id
                }
        }
        return String.EMPTY
    }

    companion object {
        const val PAGE_SIZE = 500
    }
}
