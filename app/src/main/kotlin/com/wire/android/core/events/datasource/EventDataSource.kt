package com.wire.android.core.events.datasource

import com.tinder.scarlet.WebSocket
import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.events.datasource.remote.Payload
import com.wire.android.core.events.datasource.remote.WebSocketService
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventDataSource(
    private val webSocketService: WebSocketService,
    private val externalScope: CoroutineScope,
    private val notificationLocalDataSource: NotificationLocalDataSource,
    private val notificationRemoteDataSource: NotificationRemoteDataSource
) : EventRepository {
    override fun events(): Flow<Event> = callbackFlow {
        externalScope.launch {
            webSocketService.observeWebSocketEvent().collect {
                if (it is WebSocket.Event.OnConnectionOpened<*>) {
                    val notificationId = lastNotificationId("8ccdab56ec2156ad")
                    var hasMore = true
                    while (hasMore) {
                        notificationRemoteDataSource.notificationsByBatch(100, "8ccdab56ec2156ad", notificationId).map { notificationPageResponse ->
                            hasMore = notificationPageResponse.hasMore
                            notificationPageResponse.notifications.forEach { notificationResponse ->
                                notificationResponse.payload?.let { payloads ->
                                    payloads.forEach { payload ->
                                        trySendBlocking(eventFromPayload(payload, notificationResponse.id)
                                        )
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
                        trySendBlocking(eventFromPayload(payload, it.id))
                    }
                }
            }
        }

        awaitClose { }
    }

    private fun eventFromPayload(payload: Payload, eventId: String) : Event {
        when (payload.type) {
            NEW_MESSAGE_TYPE -> {
                if(payload.data != null) {
                    return Event.Conversation.MessageEvent(
                        eventId,
                        payload.conversation,
                        payload.data.sender,
                        payload.from,
                        payload.data.text,
                        payload.time
                    )
                }
            }
        }
        return Event.Unknown
    }

    //TODO this should be called in full state sync
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
        const val NEW_MESSAGE_TYPE = "conversation.otr-message-add"
    }
}
