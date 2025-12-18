/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutCallback
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.sync.MessageSyncRetryWorker
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This is a helper class used to collect the necessary data and perform the actions required in order for the app to function properly,
 * such as notifications or persistent web socket.
 */
@Singleton
class GlobalObserversManager @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val notificationManager: WireNotificationManager,
    private val notificationChannelsManager: NotificationChannelsManager,
    private val userDataStoreProvider: UserDataStoreProvider,
    private val currentScreenManager: CurrentScreenManager,
    private val workManager: WorkManager,
) {
    // TODO(tests): refactor so scope/dispatcher can be injected and properly stopped
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io())

    fun observe() {
        scope.launch { setUpNotifications() }
        scope.launch {
        }
        scope.launch {
            coreLogic.getGlobalScope().observeValidAccounts().distinctUntilChanged().collectLatest {
                if (it.isNotEmpty()) {
                    coreLogic.getSessionScope(it.first().first.id).calls.endCallOnConversationChange()
                }
            }
        }
        scope.handleLogouts()
        scope.handleDeleteEphemeralMessageEndDate()
        scope.handleMessageSync()
    }

    private suspend fun setUpNotifications() {
        val persistentStatusesFlow = coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus()
            .let { result ->
                when (result) {
                    is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure -> {
                        appLogger.e("Failure while fetching persistent web socket status flow from GlobalObserversManager")
                        flowOf(listOf())
                    }

                    is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                        result.persistentWebSocketStatusListFlow
                    }
                }
            }

        coreLogic.getGlobalScope().observeValidAccounts()
            .combine(persistentStatusesFlow) { list, persistentStatuses ->
                val persistentStatusesMap = persistentStatuses.associate { it.userId to it.isPersistentWebSocketEnabled }
                /*
                  Intersect both lists as they can be slightly out of sync because both lists can be updated at slightly different times.
                  When user is logged out, at this time one of them can still contain this invalid user - make sure that it's ignored.
                  When user is logged in, at this time one of them can still not contain this new user - ignore for now,
                  the user will be handled correctly in the next iteration when the second list becomes updated as well.
                 */
                list.map { (selfUser, _) -> selfUser }
                    .filter { persistentStatusesMap.containsKey(it.id) }
                    .map { it to persistentStatusesMap.getValue(it.id) }
            }
            .distinctUntilChanged()
            .collectLatest {
                // create notification channels for all valid users
                notificationChannelsManager.createUserNotificationChannels(it.map { it.first })

                // do not observe notifications for users with PersistentWebSocketEnabled, it will be done in PersistentWebSocketService
                it.filter { (_, isPersistentWebSocketEnabled) -> !isPersistentWebSocketEnabled }
                    .map { (selfUser, _) -> selfUser.id }
                    .run {
                        notificationManager.observeNotificationsAndCallsWhileRunning(this, scope)
                    }
                // it would be nice to call all the notification observations in one place,
                // but we can't start PersistentWebSocketService here, to avoid ForegroundServiceStartNotAllowedException
            }
    }

    private fun CoroutineScope.handleLogouts() {
        callbackFlow<Unit> {
            val callback: LogoutCallback = object : LogoutCallback {
                override suspend fun invoke(userId: UserId, reason: LogoutReason) {
                    notificationManager.stopObservingOnLogout(userId)
                    if (reason != LogoutReason.SELF_SOFT_LOGOUT) {
                        userDataStoreProvider.getOrCreate(userId).clear()
                    }
                }
            }
            coreLogic.getGlobalScope().logoutCallbackManager.register(callback)
            awaitClose { coreLogic.getGlobalScope().logoutCallbackManager.unregister(callback) }
        }.launchIn(this)
    }

    private fun CoroutineScope.handleDeleteEphemeralMessageEndDate() {
        launch {
            currentScreenManager.isAppVisibleFlow()
                .flatMapLatest { isAppVisible ->
                    if (isAppVisible) {
                        coreLogic.getGlobalScope().session.currentSessionFlow()
                            .distinctUntilChanged()
                            .filter { it is CurrentSessionResult.Success && it.accountInfo.isValid() }
                            .map { (it as CurrentSessionResult.Success).accountInfo.userId }
                    } else {
                        emptyFlow()
                    }
                }
                .collect { userId -> coreLogic.getSessionScope(userId).messages.deleteEphemeralMessageEndDate() }
        }
    }

    private fun CoroutineScope.handleMessageSync() {
        launch {
            var previousVisibility: Boolean? = null

            currentScreenManager.isAppVisibleFlow()
                .collectLatest { isVisible ->
                    val previous = previousVisibility

                    // Only trigger sync on transitions, not the initial state
                    if (previous != null && previous != isVisible) {
                        val transition = if (isVisible) "background → foreground" else "foreground → background"
                        appLogger.i("GlobalObserversManager: App transition detected ($transition), triggering message sync")

                        // Get current user session
                        val currentSession = coreLogic.getGlobalScope().session.currentSessionFlow().first()
                        if (currentSession is CurrentSessionResult.Success && currentSession.accountInfo.isValid()) {
                            val userId = currentSession.accountInfo.userId
                            val sessionScope = coreLogic.getSessionScope(userId)
                            val result = sessionScope.messages.syncMessages()

                            when (result) {
                                is com.wire.kalium.logic.feature.message.sync.SyncMessagesResult.Success -> {
                                    appLogger.i("GlobalObserversManager: Message sync completed successfully")
                                }
                                is com.wire.kalium.logic.feature.message.sync.SyncMessagesResult.NothingToSync -> {
                                    appLogger.i("GlobalObserversManager: No messages to sync")
                                }
                                is com.wire.kalium.logic.feature.message.sync.SyncMessagesResult.Disabled -> {
                                    appLogger.i("GlobalObserversManager: Message sync feature disabled")
                                }
                                is com.wire.kalium.logic.feature.message.sync.SyncMessagesResult.ApiFailure -> {
                                    appLogger.w("GlobalObserversManager: Message sync API failure (${result.statusCode}), enqueueing retry")
                                    enqueueMessageSyncRetry(userId)
                                }
                                is com.wire.kalium.logic.feature.message.sync.SyncMessagesResult.Failure -> {
                                    appLogger.e("GlobalObserversManager: Message sync failed, enqueueing retry", result.exception)
                                    enqueueMessageSyncRetry(userId)
                                }
                            }
                        } else {
                            appLogger.w("GlobalObserversManager: No valid user session, skipping message sync")
                        }
                    }

                    // Update previous state
                    previousVisibility = isVisible
                }
        }
    }

    private fun enqueueMessageSyncRetry(userId: UserId) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("worker_class", MessageSyncRetryWorker::class.java.canonicalName)
            .putString("user-id-worker-param", Json.encodeToString(userId))
            .build()

        val workRequest = OneTimeWorkRequestBuilder<com.wire.kalium.logic.sync.WrapperWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            MessageSyncRetryWorker.NAME_PREFIX + userId.value,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        appLogger.i("GlobalObserversManager: Message sync retry worker enqueued")
    }
}
