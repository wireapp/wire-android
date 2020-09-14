package com.wire.android.shared.activeuser.di

import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.shared.activeuser.ActiveUserRepository
import com.wire.android.shared.activeuser.datasources.ActiveUserDataSource
import com.wire.android.shared.activeuser.datasources.local.ActiveUserLocalDataSource
import org.koin.dsl.module

val activeUserModule = module {
    single<ActiveUserRepository> { ActiveUserDataSource(get()) }
    single { ActiveUserLocalDataSource(get(), get()) }
    factory { get<GlobalDatabase>().activeUserDao() }
}
