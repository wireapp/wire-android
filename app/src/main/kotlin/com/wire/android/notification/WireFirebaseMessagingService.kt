package com.wire.android.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wire.kalium.logic.CoreLogic


class MyFirebaseMessagingService(private val coreLogic: CoreLogic) : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.e("asd",p0)
        coreLogic.getAuthenticationScope().saveNotificationToken(p0,"GCM")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.e("Noti", "recieved")

    }
}
