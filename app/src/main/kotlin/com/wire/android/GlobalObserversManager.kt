package com.wire.android

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.UserAgentProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.CoroutineScope
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
class GlobalObserversManager @Inject constructor(
    dispatcherProvider: DispatcherProvider,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val userAgentProvider: UserAgentProvider,
    private val notificationManager: WireNotificationManager,
    private val navigationManager: NavigationManager,
    private val notificationChannelsManager: NotificationChannelsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io())

    fun observe() {
        scope.launch { observeAccountsToCreateChannels() }
    }

    private suspend fun observeAccountsToCreateChannels() {
        coreLogic.getGlobalScope().observeValidAccounts(userAgent = userAgentProvider.defaultUserAgent)
            .distinctUntilChanged()
            .collect { list ->
                notificationChannelsManager.createUserNotificationChannels(list.map { it.first })
                list.map {
                    it.first.id
                }.run {
                    notificationManager.observeNotificationsAndCallsWhileRunning(this, scope) {
                        openIncomingCall(it.conversationId)
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
