package com.wire.android.shared.notification.di

import com.wire.android.shared.notification.builder.NotificationBuilder
import com.wire.android.shared.notification.builder.NotificationChannelBuilder
import com.wire.android.shared.notification.builder.NotificationSummaryBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val headsUpNotificationModule = module {
    single { NotificationBuilder(androidContext(), get(), get()) }
    single { NotificationChannelBuilder(androidContext()) }
    single { NotificationSummaryBuilder(androidContext()) }
}
