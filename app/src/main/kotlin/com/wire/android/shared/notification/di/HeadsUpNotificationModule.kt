package com.wire.android.shared.notification.di

import com.wire.android.shared.notification.builder.NotificationBuilder
import com.wire.android.shared.notification.builder.NotificationChannelBuilder
import com.wire.android.shared.notification.builder.NotificationSummaryBuilder
import com.wire.android.shared.notification.usecase.ShouldDisplayNotificationUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val headsUpNotificationModule = module {
    factory { ShouldDisplayNotificationUseCase(get()) }
    single { NotificationBuilder(androidContext(), get(), get()) }
    single { NotificationChannelBuilder(androidContext()) }
    single { NotificationSummaryBuilder(androidContext()) }
}
