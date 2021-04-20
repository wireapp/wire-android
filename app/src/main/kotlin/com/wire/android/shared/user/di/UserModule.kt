package com.wire.android.shared.user.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.shared.user.UserRepository
import com.wire.android.shared.user.datasources.UserDataSource
import com.wire.android.shared.user.datasources.local.UserLocalDataSource
import com.wire.android.shared.user.datasources.remote.UserApi
import com.wire.android.shared.user.datasources.remote.UserRemoteDataSource
import com.wire.android.shared.user.mapper.UserMapper
import com.wire.android.shared.user.name.ValidateNameUseCase
import com.wire.android.shared.user.password.PasswordLengthConfig
import com.wire.android.shared.user.password.ValidatePasswordUseCase
import com.wire.android.shared.user.usecase.GetCurrentUserUseCase
import org.koin.dsl.module

val userModule = module {
    single<UserRepository> { UserDataSource(get(), get(), get(), get()) }
    single { UserLocalDataSource(get()) }
    factory { get<GlobalDatabase>().userDao() }

    single { UserRemoteDataSource(get(), get()) }
    factory { get<NetworkClient>().create(UserApi::class.java) }

    single { UserMapper() }

    factory { ValidateNameUseCase() }
    factory { ValidatePasswordUseCase(get()) }

    factory { PasswordLengthConfig() }

    factory { GetCurrentUserUseCase(get(), get()) }
}
