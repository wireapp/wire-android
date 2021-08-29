package com.wire.android.shared.notification.di

import com.wire.android.core.storage.db.user.UserDatabase
import org.koin.dsl.module

val notificationModule = module {
    factory { get<UserDatabase>().notificationDao() }
}
