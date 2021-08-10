package com.wire.android.core.events.datasource

import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class EventDataSource(
    private val externalScope: CoroutineScope,
    private val notificationLocalDataSource: NotificationLocalDataSource,
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val clientId: String
) : EventRepository {
    override fun events(): Flow<Event> = callbackFlow {
        externalScope.launch {
            notificationRemoteDataSource.receiveEvents().collect { events ->
                events?.forEach {
                    trySendBlocking(it)
                }
            }
        }

        externalScope.launch {
            val notificationId = lastNotificationId(clientId)
            notificationRemoteDataSource.notificationsFlow(clientId, notificationId).collect { events ->
                events.forEach {
                    trySendBlocking(it)
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
}
