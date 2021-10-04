package com.wire.android.core.events.datasource

import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.onSuccess
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

//TODO missing unit test
class EventDataSource(
    private val notificationLocalDataSource: NotificationLocalDataSource,
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val sessionRepository: SessionRepository
) : EventRepository {

    @ExperimentalCoroutinesApi
    override fun events(): Flow<Either<Failure, Event>> = channelFlow {
        sessionRepository.currentClientId().onSuccess { clientId ->
            launch {
                notificationRemoteDataSource.receiveEvents(clientId).collect { events ->
                    events?.forEach { send(Either.Right(it)) }
                }

                lastNotificationId(clientId).map { notificationId ->
                    launch {
                        notificationRemoteDataSource.notificationsFlow(clientId, notificationId).collect { events ->
                            events.forEach { send(Either.Right(it)) }
                        }
                    }
                }
            }
        }
        awaitClose()
    }

    //TODO this function should be moved to be called in full state sync
    private suspend fun lastNotificationId(clientId: String): Either<Failure, String> {
        return notificationLocalDataSource.lastNotificationId()?.let {
            Either.Right(it)
        } ?: run {
            notificationRemoteDataSource.lastNotification(clientId).map { notificationResponse ->
                notificationLocalDataSource.saveLastNotificationId(notificationResponse.id)
                return@map notificationResponse.id
            }
        }
    }
}
