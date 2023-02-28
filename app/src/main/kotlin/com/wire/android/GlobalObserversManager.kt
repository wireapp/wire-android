package com.wire.android

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
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
    private val notificationChannelsManager: NotificationChannelsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    
    fun observe() {
        scope.launch { observeAccountsToCreateChannels() }
    }

    private suspend fun observeAccountsToCreateChannels() {
        coreLogic.getGlobalScope().observeValidAccounts()
            .distinctUntilChanged()
            .collect { list ->
                notificationChannelsManager.createUserNotificationChannels(list.map { it.first })
            }
    }
}
