package com.wire.android.shared.session.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.session.datasources.SessionDataSource
import com.wire.android.shared.session.datasources.local.SessionLocalDataSource
import com.wire.android.shared.session.datasources.remote.SessionApi
import com.wire.android.shared.session.datasources.remote.SessionRemoteDataSource
import com.wire.android.shared.session.mapper.SessionMapper
import org.koin.dsl.module

val sessionModule = module {
    single<SessionRepository> { SessionDataSource(get(), get(), get()) }
    single { SessionLocalDataSource(get()) }
    factory { get<GlobalDatabase>().sessionDao() }
    single { SessionRemoteDataSource(get(), get()) }
    single { get<NetworkClient>().create(SessionApi::class.java) }
    factory { SessionMapper() }
}
