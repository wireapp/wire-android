package com.wire.android.feature.auth.di

import com.wire.android.R
import com.wire.android.core.network.NetworkClient
import com.wire.android.core.ui.navigation.FragmentContainerProvider
import com.wire.android.feature.auth.activation.ActivationRepository
import com.wire.android.feature.auth.activation.datasource.ActivationDataSource
import com.wire.android.feature.auth.activation.datasource.remote.ActivationApi
import com.wire.android.feature.auth.activation.datasource.remote.ActivationRemoteDataSource
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.datasource.ClientDataSource
import com.wire.android.feature.auth.client.datasource.remote.ClientRemoteDataSource
import com.wire.android.feature.auth.client.datasource.remote.api.ClientApi
import com.wire.android.feature.auth.client.mapper.ClientMapper
import com.wire.android.feature.auth.client.ui.DeviceLimitActivity
import com.wire.android.feature.auth.client.ui.DeviceLimitViewModel
import com.wire.android.feature.auth.client.usecase.RegisterClientUseCase
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.feature.auth.login.email.datasource.LoginDataSource
import com.wire.android.feature.auth.login.email.datasource.remote.LoginApi
import com.wire.android.feature.auth.login.email.datasource.remote.LoginRemoteDataSource
import com.wire.android.feature.auth.login.email.ui.LoginWithEmailViewModel
import com.wire.android.feature.auth.login.email.usecase.LoginWithEmailUseCase
import com.wire.android.feature.auth.login.ui.navigation.LoginNavigator
import com.wire.android.feature.auth.registration.CreateAccountActivity
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.feature.auth.registration.datasource.RegistrationDataSource
import com.wire.android.feature.auth.registration.datasource.remote.RegistrationApi
import com.wire.android.feature.auth.registration.datasource.remote.RegistrationRemoteDataSource
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountNameViewModel
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountPasswordViewModel
import com.wire.android.feature.auth.registration.personal.usecase.ActivateEmailUseCase
import com.wire.android.feature.auth.registration.personal.usecase.RegisterPersonalAccountUseCase
import com.wire.android.feature.auth.registration.pro.team.CreateProAccountTeamNameViewModel
import com.wire.android.feature.auth.registration.pro.team.data.TeamDataSource
import com.wire.android.feature.auth.registration.pro.team.data.TeamsRepository
import com.wire.android.feature.auth.registration.pro.team.usecase.GetTeamNameUseCase
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameUseCase
import com.wire.android.feature.auth.registration.ui.CreateAccountEmailVerificationCodeViewModel
import com.wire.android.feature.auth.registration.ui.CreateAccountEmailViewModel
import com.wire.android.feature.auth.registration.ui.CreateAccountUsernameViewModel
import com.wire.android.feature.auth.registration.ui.navigation.CreateAccountNavigator
import com.wire.android.shared.auth.remote.LabelGenerator
import com.wire.android.shared.session.usecase.SetSessionCurrentUseCase
import com.wire.android.shared.user.email.ValidateEmailUseCase
import com.wire.android.shared.user.username.CheckUsernameExistsUseCase
import com.wire.android.shared.user.username.GenerateRandomUsernameUseCase
import com.wire.android.shared.user.username.UpdateUsernameUseCase
import com.wire.android.shared.user.username.UsernameAttemptsGenerator
import com.wire.android.shared.user.username.ValidateUsernameUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val authenticationModules
    get() = arrayOf(
        authenticationCommonModule,
        createAccountModule,
        createPersonalAccountModule,
        createProAccountModule,
        loginModule,
        clientModule
    )

private val authenticationCommonModule = module {
    factory { LabelGenerator() }
}

private val createAccountModule = module {
    single<RegistrationRepository> { RegistrationDataSource(get(), get(), get()) }
    factory { get<NetworkClient>().create(RegistrationApi::class.java) }
    factory { RegistrationRemoteDataSource(get(), get(), get(), get()) }

    viewModel { CreateAccountEmailViewModel(get(), get(), get()) }
    viewModel { CreateAccountEmailVerificationCodeViewModel(get(), get()) }
    viewModel { CreateAccountUsernameViewModel(get(), get(), get(), get(), get()) }

    factory { ActivateEmailUseCase(get()) }
    single<ActivationRepository> { ActivationDataSource(get()) }
    single { ActivationRemoteDataSource(get(), get()) }
    factory { get<NetworkClient>().create(ActivationApi::class.java) }

    factory { ValidateEmailUseCase() }
    factory { ValidateUsernameUseCase() }
    factory { UpdateUsernameUseCase(get(), get()) }
    factory { CheckUsernameExistsUseCase(get()) }
    factory { SendEmailActivationCodeUseCase(get()) }
    factory { GenerateRandomUsernameUseCase(get(), get(), get()) }
    factory {
        UsernameAttemptsGenerator(
            androidContext().resources.getStringArray(R.array.username_generation_adjectives),
            androidContext().resources.getStringArray(R.array.username_generation_random_words)
        )
    }

    single { CreateAccountNavigator(get(), get()) }
    factory(qualifier<CreateAccountActivity>()) {
        FragmentContainerProvider.fixedProvider(R.id.createAccountLayoutContainer)
    }
}

private val createPersonalAccountModule = module {
    viewModel { CreatePersonalAccountNameViewModel(get(), get()) }
    viewModel { CreatePersonalAccountPasswordViewModel(get(), get(), get()) }
    factory { RegisterPersonalAccountUseCase(get(), get(), get()) }
}

private val createProAccountModule = module {
    viewModel { CreateProAccountTeamNameViewModel(get(), get(), get()) }
    factory { GetTeamNameUseCase(get()) }
    factory { UpdateTeamNameUseCase(get()) }
    single { TeamDataSource() as TeamsRepository }
}

private val loginModule = module {
    single { LoginNavigator(get(), get(), get()) }
    viewModel { LoginWithEmailViewModel(get(), get(), get()) }
    factory { LoginWithEmailUseCase(get(), get(), get()) }

    single<LoginRepository> { LoginDataSource(get(), get()) }
    single { LoginRemoteDataSource(get(), get(), get()) }
    factory { get<NetworkClient>().create(LoginApi::class.java) }
}

private val clientModule = module {
    factory(qualifier<DeviceLimitActivity>()) {
        FragmentContainerProvider.fixedProvider(R.id.deviceLimitFragmentContainer)
    }
    factory { get<NetworkClient>().create(ClientApi::class.java) }
    single { ClientRemoteDataSource(get(), get()) }
    single<ClientRepository> { ClientDataSource(get(), get(), get()) }
    factory { RegisterClientUseCase(get(), get(), get()) }
    factory { ClientMapper(get(), get(), get()) }
    factory { SetSessionCurrentUseCase(get()) }
    viewModel { DeviceLimitViewModel(get(), get(), get()) }
}
