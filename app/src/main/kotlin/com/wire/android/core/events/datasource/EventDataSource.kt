package com.wire.android.core.events.datasource

import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.onFailure
import com.wire.android.core.functional.onSuccess
import com.wire.android.core.functional.suspending
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

//TODO missing unit test
class EventDataSource(
    private val externalScope: CoroutineScope,
    private val notificationLocalDataSource: NotificationLocalDataSource,
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val sessionRepository: SessionRepository
) : EventRepository {

    override fun events(): Flow<Either<Failure, Event>> = flow {
        sessionRepository.currentClientId().onSuccess { clientId ->
            externalScope.launch {
                notificationRemoteDataSource.receiveEvents(clientId).collect { events ->
                    events?.forEach {
                        emit(Either.Right(it))
                    }
                }
            }
            externalScope.launch {
                suspending {
                    lastNotificationId(clientId).map { notificationId ->
                        notificationRemoteDataSource.notificationsFlow(clientId, notificationId)
                            .collect { events ->
                                events.forEach {
                                    emit(Either.Right(it))
                                }
                            }
                    }
                }
            }
        }
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
