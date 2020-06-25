package com.wire.android.feature.auth.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.feature.auth.activation.ActivationRepository
import com.wire.android.feature.auth.activation.datasource.ActivationDataSource
import com.wire.android.feature.auth.activation.datasource.remote.ActivationApi
import com.wire.android.feature.auth.activation.datasource.remote.ActivationRemoteDataSource
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.feature.auth.registration.personal.email.CreatePersonalAccountEmailViewModel
import com.wire.android.feature.auth.registration.pro.team.CreateProAccountTeamNameViewModel
import com.wire.android.feature.auth.registration.pro.team.data.TeamDataSource
import com.wire.android.feature.auth.registration.pro.team.data.TeamsRepository
import com.wire.android.feature.auth.registration.pro.team.usecase.GetTeamNameUseCase
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameUseCase
import com.wire.android.shared.user.email.ValidateEmailUseCase
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val authenticationModules: List<Module>
    get() = listOf(
        createPersonalAccountModule,
        createProAccountModule
    )

private val createPersonalAccountModule: Module = module {
    viewModel { CreatePersonalAccountEmailViewModel(get(), get()) }
    factory { ValidateEmailUseCase() }
    factory { SendEmailActivationCodeUseCase(get()) }
    single<ActivationRepository> { ActivationDataSource(get()) }
    single { ActivationRemoteDataSource(get(), get()) }
    factory { get<NetworkClient>().create(ActivationApi::class.java) }
}

private val createProAccountModule: Module = module {
    viewModel { CreateProAccountTeamNameViewModel(get(), get()) }
    factory { GetTeamNameUseCase(get()) }
    factory { UpdateTeamNameUseCase(get()) }
    factory { TeamDataSource() as TeamsRepository }
}
