package com.wire.android.shared.user.di

import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase
import com.wire.android.shared.user.UserRepository
import com.wire.android.shared.user.datasources.UserDataSource
import com.wire.android.shared.user.datasources.local.UserLocalDataSource
import com.wire.android.shared.user.name.ValidateNameUseCase
import com.wire.android.shared.user.password.PasswordLengthConfig
import com.wire.android.shared.user.password.ValidatePasswordUseCase
import org.koin.dsl.module

val userModule = module {
    single<UserRepository> { UserDataSource(get()) }
    single { UserLocalDataSource(get()) }
    factory { get<GlobalDatabase>().userDao() }

    factory { ValidateNameUseCase() }
    factory { ValidatePasswordUseCase(get()) }

    factory { PasswordLengthConfig() }

    factory { GetActiveUserUseCase() }
}
