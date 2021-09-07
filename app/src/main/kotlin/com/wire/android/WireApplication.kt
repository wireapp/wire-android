package com.wire.android

import android.app.Application
import com.wire.android.core.di.Injector
import com.wire.android.shared.notification.builder.NotificationBuilder
import com.wire.android.shared.notification.builder.NotificationChannelBuilder
import com.wire.android.shared.notification.builder.NotificationSummaryBuilder
import org.koin.android.ext.android.inject

class WireApplication : Application() {

    private val notificationChannelBuilder by inject<NotificationChannelBuilder>()

    override fun onCreate() {
        super.onCreate()
        Injector.start(this)

        notificationChannelBuilder.createChannel(
            NotificationSummaryBuilder.NOTIFICATIONS_CHANNEL_ID,
            NotificationSummaryBuilder.NOTIFICATIONS_CHANNEL_NAME,
            NotificationSummaryBuilder.NOTIFICATIONS_CHANNEL_DESCRIPTION
        )

        notificationChannelBuilder.createChannel(
            NotificationBuilder.NOTIFICATION_MESSAGE_CHANNEL_ID,
            NotificationBuilder.NOTIFICATION_MESSAGE_CHANNEL_NAME,
            NotificationBuilder.NOTIFICATION_MESSAGE_CHANNEL_DESCRIPTION
        )
    }
}
