package com.wire.android.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        coreLogic.getAuthenticationScope().saveNotificationToken(p0, "GCM")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        //todo: wake up the websocket to receive the notification on the device
    }
}
