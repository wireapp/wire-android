package com.wire.android.shared.activeusers.di

import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.shared.activeusers.ActiveUsersRepository
import com.wire.android.shared.activeusers.datasources.ActiveUsersDataSource
import com.wire.android.shared.activeusers.datasources.local.ActiveUsersLocalDataSource
import org.koin.dsl.module

val activeUsersModule = module {
    single<ActiveUsersRepository> { ActiveUsersDataSource(get()) }
    single { ActiveUsersLocalDataSource(get()) }
    factory { get<GlobalDatabase>().activeUsersDao() }
}
