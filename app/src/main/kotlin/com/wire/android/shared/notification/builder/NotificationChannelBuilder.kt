package com.wire.android.shared.notification.builder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NotificationChannelBuilder(private val applicationContext: Context) {

    fun createChannel(channelId: String, channelName: String, channelDescription: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { setShowBadge(true) }

            notificationChannel.enableVibration(true)
            notificationChannel.description = channelDescription

            val notificationManager = applicationContext.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
