package com.wire.android.core.events.datasource

import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

//TODO missing unit test
class EventDataSource(
    private val externalScope: CoroutineScope,
    private val notificationLocalDataSource: NotificationLocalDataSource,
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val sessionRepository: SessionRepository
) : EventRepository {

    override fun events(): Flow<Either<Failure, Event>> = callbackFlow {
        sessionRepository.currentClientId().onSuccess { clientId ->
            externalScope.launch {
                notificationRemoteDataSource.receiveEvents(clientId).collect { events ->
                    events?.forEach {
                        trySendBlocking(Either.Right(it))
                    }
                }
            }

            externalScope.launch {
                val notificationId = lastNotificationId(clientId)
                notificationRemoteDataSource.notificationsFlow(clientId, notificationId)
                    .collect { events ->
                        events.forEach {
                            trySendBlocking(Either.Right(it))
                        }
                    }
            }
        }.onFailure {
            trySend(Either.Left(it))
        }

        awaitClose { }
    }

    //TODO this function should be moved to be called in full state sync
    private suspend fun lastNotificationId(clientId: String): String {
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
