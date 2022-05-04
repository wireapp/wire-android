package com.wire.android.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class WireFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var wireNotificationManager: WireNotificationManager

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        coreLogic.getAuthenticationScope().saveNotificationToken(p0, "GCM")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        var userIdValue = ""
        for (items in message.data) {
            if (items.key == "user") {
                userIdValue = items.value
                break
            }
        }
        runBlocking {
            wireNotificationManager.fetchAndShowMessageNotificationsOnce(userIdValue)
        }
    }
}
