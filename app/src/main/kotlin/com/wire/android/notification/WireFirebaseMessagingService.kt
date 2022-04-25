package com.wire.android.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.e("asdasd","asdasdsdsadasd")

        Log.e("asdasd",p0)
    }

}
