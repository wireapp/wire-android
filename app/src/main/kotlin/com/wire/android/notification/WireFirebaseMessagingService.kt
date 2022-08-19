package com.wire.android.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.notificationToken.SaveNotificationTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WireFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var wireNotificationManager: WireNotificationManager

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val scope by lazy {
        // There's no UI, no need to run anything using the Main/UI Dispatcher
        CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        appLogger.i("$TAG: notification received")
        var userIdValue = ""
        for (items in message.data) {
            if (items.key == "user") {
                userIdValue = items.value
                break
            }
        }
        scope.launch {
            wireNotificationManager.fetchAndShowNotificationsOnce(userIdValue)
        }
    }

    override fun onDestroy() {
        scope.cancel("WireFirebaseMessagingService was destroyed")
        super.onDestroy()
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        scope.launch {
            coreLogic.globalScope {
                saveNotificationToken(p0, "GCM")
            }.let { result ->
                when (result) {
                    is SaveNotificationTokenUseCase.Result.Failure.Generic -> {
                        appLogger.e("$TAG: token registration has an issue : ${result.failure} ")

                    }

                    SaveNotificationTokenUseCase.Result.Success -> {
                        appLogger.i("$TAG: token registered successfully")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WireFirebaseMessagingService"
    }
}
