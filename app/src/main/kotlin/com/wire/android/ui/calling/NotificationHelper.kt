package com.wire.android.ui.calling

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.wire.android.WireApplication

val Context.notificationManager: NotificationManager get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

object NotificationHelper {
    private fun createContentIntent(context: Context): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return if (launchIntent != null) {
            launchIntent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            val fallbackIntent = Intent(context, CallActivity::class.java)
            PendingIntent.getActivity(
                context,
                0,
                fallbackIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }

    fun generateId(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }

    fun ensureDefaultChannel() {
        val notificationManager = WireApplication.instance.notificationManager
        if (notificationManager.getNotificationChannel("screen_mirror") == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    "screen_mirror",
                    "Wire Screen Mirror",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    setShowBadge(false)
                },
            )
        }
    }

    fun createServiceNotification(
        context: Context,
        action: String,
        title: String,
        description: String = "",
    ): Notification {
        val stopPendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ServiceStopBroadcastReceiver::class.java).apply {
                    this.action = action
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        return NotificationCompat.Builder(context, "screen_mirror").apply {
//            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification))
            setContentTitle(title)
            setContentText(description)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setOnlyAlertOnce(true)
            setSilent(true)
            setWhen(System.currentTimeMillis())
            setAutoCancel(false)
            setContentIntent(createContentIntent(context))
            addAction(-1, "stop service", stopPendingIntent)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }.build()
    }
}
