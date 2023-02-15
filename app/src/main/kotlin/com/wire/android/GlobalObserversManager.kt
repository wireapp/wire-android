package com.wire.android

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.services.ServicesManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This is a helper class used to collect the necessary data and perform the actions required in order for the app to function properly,
 * such as notifications or persistent web socket.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class GlobalObserversManager @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val notificationChannelsManager: NotificationChannelsManager,
    private val notificationManager: WireNotificationManager,
    private val servicesManager: ServicesManager,
    private val navigationManager: NavigationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io())

    fun observe() {
        scope.launch { observeAccountsToCreateChannels() }
        scope.launch { observePersistentConnectionStatusToRunWebSocketAndNotifications() }
    }

    private suspend fun observeAccountsToCreateChannels() {
        coreLogic.getGlobalScope().observeValidAccounts()
            .distinctUntilChanged()
            .collect { list ->
                notificationChannelsManager.createUserNotificationChannels(list.map { it.first })
            }
    }

    private suspend fun observePersistentConnectionStatusToRunWebSocketAndNotifications() {
        coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus().let { result ->
            when (result) {
                is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure -> {
                    appLogger.e("Failure while fetching persistent web socket status flow from wire activity")
                }
                is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                    result.persistentWebSocketStatusListFlow.collect { statuses ->
                        val usersToObserve = statuses
                            .filter { !it.isPersistentWebSocketEnabled }
                            .map { it.userId }

                        notificationManager.observeNotificationsAndCallsWhileRunning(
                            usersToObserve,
                            scope
                        ) { call -> openIncomingCall(call.conversationId) }

                        if (statuses.any { it.isPersistentWebSocketEnabled }) {
                            if (!servicesManager.isPersistentWebSocketServiceRunning()) {
                                servicesManager.startPersistentWebSocketService()
                            }
                        } else {
                            servicesManager.stopPersistentWebSocketService()
                        }
                    }
                }
            }
        }
    }

    private fun openIncomingCall(conversationId: ConversationId) {
        scope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.IncomingCall.getRouteWithArgs(listOf(conversationId))))
        }
    }
}
