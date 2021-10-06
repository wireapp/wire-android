package com.wire.android.core.events.datasource

import com.wire.android.core.events.Event
import com.wire.android.core.events.EventRepository
import com.wire.android.core.events.datasource.local.NotificationLocalDataSource
import com.wire.android.core.events.datasource.remote.NotificationRemoteDataSource
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class EventDataSource(
    private val notificationLocalDataSource: NotificationLocalDataSource,
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val sessionRepository: SessionRepository
) : EventRepository {

    override suspend fun events(): Flow<Either<Failure, Event>> {
        val currentClientIdFlow = flowOf(sessionRepository.currentClientId()).mapNotNull { currentId ->
            currentId.fold({ null }, { it })
        }

        val pendingEventsFlow = currentClientIdFlow.mapNotNull { clientId ->
            val notificationId = lastNotificationId(clientId).fold({ null }, { it })
            notificationId?.let { clientId to it }
        }.flatMapMerge {
            notificationRemoteDataSource.notificationsFlow(it.first, it.second)
        }.flatMapMerge { it.asFlow() }

        val liveEventsFlow = currentClientIdFlow.flatMapMerge { clientId ->
            notificationRemoteDataSource.receiveEvents(clientId)
        }.filterNotNull().flatMapMerge {
            it.asFlow()
        }

        return flowOf(pendingEventsFlow, liveEventsFlow).flattenConcat().map { Either.Right(it) }
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
