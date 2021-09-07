package com.wire.android.shared.notification.builder

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.R

class NotificationSummaryBuilder(private val applicationContext: Context) {

    fun createSummaryNotification() {
        val summaryNotification =
            NotificationCompat.Builder(applicationContext, NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setGroup(NotificationBuilder.GROUP_KEY_WIRE_NOTIFICATIONS)
                .setGroupSummary(true)
                .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

    companion object {
        private const val SUMMARY_NOTIFICATION_ID = 0
        const val NOTIFICATIONS_CHANNEL_ID = "notifications_id"
        const val NOTIFICATIONS_CHANNEL_NAME = "notifications"
        const val NOTIFICATIONS_CHANNEL_DESCRIPTION = "All notifications"
    }
}
