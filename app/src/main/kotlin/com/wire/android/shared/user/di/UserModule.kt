package com.wire.android.shared.user.di

import com.wire.android.shared.user.name.ValidateNameUseCase
import com.wire.android.shared.user.password.PasswordLengthConfig
import com.wire.android.shared.user.password.ValidatePasswordUseCase
import org.koin.dsl.module

val userModule = module {
    factory { ValidateNameUseCase() }
    factory { ValidatePasswordUseCase(get()) }

    factory { PasswordLengthConfig() }
}
