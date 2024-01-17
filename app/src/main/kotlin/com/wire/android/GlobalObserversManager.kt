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
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutCallback
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
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
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io())

    fun observe() {
        scope.launch { setUpNotifications() }
        scope.launch {
            coreLogic.getGlobalScope().observeValidAccounts().distinctUntilChanged().collectLatest {
                if (it.isNotEmpty()) {
                    coreLogic.getSessionScope(it.first().first.id).calls.endCallOnConversationChange()
                }
            }
        }
        scope.handleLogouts()
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
            .distinctUntilChanged()
            .combine(persistentStatusesFlow, ::Pair)
            .collect { (list, persistentStatuses) ->
                notificationChannelsManager.createUserNotificationChannels(list.map { it.first })

                list.map { it.first.id }
                    // do not observe notifications for users with PersistentWebSocketEnabled, it will be done in PersistentWebSocketService
                    .filter { userId -> persistentStatuses.none { it.userId == userId && it.isPersistentWebSocketEnabled } }
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
                    notificationChannelsManager.deleteChannelGroup(userId)
                    if (reason != LogoutReason.SELF_SOFT_LOGOUT) {
                        userDataStoreProvider.getOrCreate(userId).clear()
                    }
                }
            }
            coreLogic.getGlobalScope().logoutCallbackManager.register(callback)
            awaitClose { coreLogic.getGlobalScope().logoutCallbackManager.unregister(callback) }
        }.launchIn(this)
    }
}
