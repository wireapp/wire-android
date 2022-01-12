package com.wire.android

import android.app.Application
import com.wire.android.core.di.Injector
import com.wire.android.shared.notification.builder.NotificationBuilder
import com.wire.android.shared.notification.builder.NotificationChannelBuilder
import org.koin.android.ext.android.inject
import org.koin.core.context.stopKoin

class WireApplication : Application() {

    private val notificationChannelBuilder by inject<NotificationChannelBuilder>()

    override fun onCreate() {
        super.onCreate()
        Injector.start(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {

        notificationChannelBuilder.createChannel(
            NotificationBuilder.NOTIFICATION_MESSAGE_CHANNEL_ID,
            getString(R.string.notification_message_channel_name),
            getString(R.string.notification_message_channel_description)
        )
    }
}
