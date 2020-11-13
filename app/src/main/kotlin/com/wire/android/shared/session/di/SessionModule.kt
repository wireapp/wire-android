package com.wire.android.shared.session.di

import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.session.datasources.SessionDataSource
import com.wire.android.shared.session.datasources.local.SessionLocalDataSource
import com.wire.android.shared.session.datasources.remote.SessionRemoteDataSource
import com.wire.android.shared.session.mapper.SessionMapper
import com.wire.android.shared.session.usecase.CheckCurrentSessionExistsUseCase
import org.koin.dsl.module

val sessionModule = module {
    single<SessionRepository> { SessionDataSource(get(), get(), get()) }
    factory { SessionLocalDataSource(get()) }
    factory { get<GlobalDatabase>().sessionDao() }
    factory { SessionRemoteDataSource(get(), get()) }

    factory { SessionMapper() }
    factory { CheckCurrentSessionExistsUseCase(get()) }
}
