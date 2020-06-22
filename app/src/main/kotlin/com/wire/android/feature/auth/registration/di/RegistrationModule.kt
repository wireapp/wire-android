package com.wire.android.feature.auth.registration.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.feature.auth.activation.ActivationRepository
import com.wire.android.feature.auth.activation.datasource.ActivationDataSource
import com.wire.android.feature.auth.activation.datasource.remote.ActivationApi
import com.wire.android.feature.auth.activation.datasource.remote.ActivationRemoteDataSource
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.feature.auth.registration.personal.email.CreatePersonalAccountEmailViewModel
import com.wire.android.shared.user.email.ValidateEmailUseCase
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val registrationModule: Module = module {
    viewModel { CreatePersonalAccountEmailViewModel(get(), get()) }
    factory { ValidateEmailUseCase() }
    factory { SendEmailActivationCodeUseCase(get()) }
    single<ActivationRepository> { ActivationDataSource(get()) }
    single { ActivationRemoteDataSource(get(), get()) }
    factory { get<NetworkClient>().create(ActivationApi::class.java) }
}
