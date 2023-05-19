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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
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
    private val navigationManager: NavigationManager,
    private val notificationChannelsManager: NotificationChannelsManager,
    private val servicesManager: ServicesManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io())

    fun observe() {
        scope.launch { observeAccountsToCreateChannelsAndObserveNotifications() }
    }

    private suspend fun observeAccountsToCreateChannelsAndObserveNotifications() {
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
                    .filter { userId -> persistentStatuses.none { it.userId == userId && it.isPersistentWebSocketEnabled } }
                    .run {
                        notificationManager.observeNotificationsAndCallsWhileRunning(this, scope) {
                            openIncomingCall(it.conversationId)
                        }
                    }

                if (persistentStatuses.any { it.isPersistentWebSocketEnabled }) {
                    if (!servicesManager.isPersistentWebSocketServiceRunning()) {
                        servicesManager.startPersistentWebSocketService()
                    }
                } else {
                    servicesManager.stopPersistentWebSocketService()
                }
            }
    }

    private fun openIncomingCall(conversationId: ConversationId) {
        scope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.IncomingCall.getRouteWithArgs(listOf(conversationId))))
        }
    }
}
