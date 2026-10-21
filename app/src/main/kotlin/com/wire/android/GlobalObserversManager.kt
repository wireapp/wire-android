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

import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.MeetingReminderScheduler
import com.wire.android.notification.WireNotificationManager
import com.wire.android.services.SendPendingMessagesAfterForegroundSyncUseCase
import com.wire.android.session.observeCurrentValidSession
import com.wire.android.session.runWithValidSession
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutCallback
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn

/**
 * This is a helper class used to collect the necessary data and perform the actions required in order for the app to function properly,
 * such as notifications or persistent web socket.
 */
@Suppress("LongParameterList")
@SingleIn(AppScope::class)
class GlobalObserversManager @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val notificationManager: WireNotificationManager,
    private val meetingReminderScheduler: MeetingReminderScheduler,
    private val notificationChannelsManager: NotificationChannelsManager,
    private val userDataStoreProvider: UserDataStoreProvider,
    private val currentScreenManager: CurrentScreenManager,
    private val sendPendingMessagesAfterForegroundSync: SendPendingMessagesAfterForegroundSyncUseCase,
    private val networkStateObserver: NetworkStateObserver,
    private val kaliumConfigs: KaliumConfigs,
) {
    // TODO(tests): refactor so scope/dispatcher can be injected and properly stopped
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io())

    fun observe() {
        scope.launch { meetingReminderScheduler.observe() }
        scope.launch { setUpNotifications() }
        scope.launch {
            coreLogic.getGlobalScope().observeAllValidSessionsFlow().collectLatest { result ->
                coroutineScope {
                    (result as? GetAllSessionsResult.Success)?.sessions.orEmpty().filter { it.isValid() }.forEach { account ->
                        launch {
                            coreLogic.observeValidSession(account.userId) {
                                flow<Unit> { calls.endCallOnConversationChange() }
                            }.collect {}
                        }
                    }
                }
            }
        }
        scope.handleLogouts()
        scope.handleDeleteEphemeralMessageEndDate()

        if (kaliumConfigs.pendingMessages) {
            scope.retryPendingMessagesOnAppOpen()
        }
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

        combine(
            coreLogic.getGlobalScope().observeAllValidSessionsFlow(),
            coreLogic.getGlobalScope().observeValidAccounts(),
            persistentStatusesFlow,
        ) { sessions, accounts, statuses ->
            val userIds = (sessions as? GetAllSessionsResult.Success)?.sessions.orEmpty()
                .filter { it.isValid() }.map { it.userId }
            val persistent = statuses.associate { it.userId to it.isPersistentWebSocketEnabled }
            val users = accounts.map { it.first }.filter { it.id in userIds && persistent.containsKey(it.id) }
            Triple(userIds, users, persistent)
        }
            .distinctUntilChanged()
            .collectLatest { (userIds, users, persistent) ->
                // Admitted self-user values supply channel metadata; only raw accounts authorize global clearing.
                notificationChannelsManager.createUserNotificationChannels(users)
                if (userIds.isEmpty()) {
                    appLogger.i("GlobalObserversManager: no valid users, stopping observing notifications")
                    notificationManager.clearWhenNoUsers()
                } else {
                    notificationManager.observeNotificationsAndCallsWhileRunning(
                        userIds.filter { persistent[it] == false },
                        scope,
                    )
                }
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
            coreLogic.getGlobalScope().session.currentSessionFlow()
                .observeCurrentValidSession(coreLogic) {
                    currentScreenManager.isAppVisibleFlow().filter { it }.map { messages.deleteEphemeralMessageEndDate() }
                }.collect {}
        }
    }

    private fun CoroutineScope.retryPendingMessagesOnAppOpen() {
        launch {
            currentScreenManager.isAppVisibleFlow().collectLatest { isVisible ->
                if (!isVisible) return@collectLatest
                if (networkStateObserver.observeNetworkState().value !is NetworkState.ConnectedWithInternet) {
                    appLogger.i("$TAG: no internet connection, skipping pending messages retry on app open")
                    return@collectLatest
                }
                coreLogic.getGlobalScope().session.currentSessionFlow().flatMapLatest { result ->
                    flow {
                        val account = (result as? CurrentSessionResult.Success)?.accountInfo
                        if (account?.isValid() == true) {
                            coreLogic.runWithValidSession(account.userId) {
                                sendPendingMessagesAfterForegroundSync(account.userId, this)
                            }
                        }
                        emit(Unit)
                    }
                }.first()
            }
        }
    }

    private companion object {
        private const val TAG = "GlobalObserversManager"
    }
}
