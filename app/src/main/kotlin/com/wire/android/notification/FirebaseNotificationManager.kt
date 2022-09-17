package com.wire.android.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R
import com.wire.android.appLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseNotificationManager @Inject constructor(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    var count: Int = 0;

    fun showNotification() {
        count += 1;
        appLogger.i("$TAG: showing notification $count")
        createNotificationChannel()
        notificationManager.notify("firebase${count}".hashCode(), getNotification(count))
    }

    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.FIREBASE_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_MAX)
            .setName(NotificationConstants.FIREBASE_CHANNEL_NAME)
            .build()

        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun getNotification(index: Int): Notification {

        return NotificationCompat.Builder(context, NotificationConstants.FIREBASE_CHANNEL_ID).apply {
            setDefaults(NotificationCompat.DEFAULT_ALL)

            priority = NotificationCompat.PRIORITY_MAX
            setCategory(NotificationCompat.CATEGORY_MESSAGE)
            setContentText("Notification $index")
            setSmallIcon(R.drawable.notification_icon_small)
            setGroup(NotificationConstants.FIREBASE_GROUP_KEY)
            setAutoCancel(true)

        }.build()
    }

    companion object {
        private const val TAG = "FirebaseNotificationManager"
    }
}
