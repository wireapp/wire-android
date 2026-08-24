/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.di.metro

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import com.wire.android.ui.WireActivityViewModel
import com.wire.android.ui.authentication.AuthenticationManualViewModelFactory
import com.wire.android.ui.authentication.create.code.AppCreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewModelHostFactory
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModelHostFactory
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModelHostFactory
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModelHostFactory
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.authentication.login.email.LoginEmailViewModelHostFactory
import com.wire.android.ui.authentication.login.sso.AppLoginSSOViewModel
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelHostFactory
import com.wire.android.ui.authentication.welcome.WelcomeNavArgs
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.authentication.welcome.WelcomeViewModelHostFactory
import com.wire.android.ui.newauthentication.login.AppNewLoginViewModel
import com.wire.android.ui.newauthentication.login.NewLoginViewModelHostFactory
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeViewModel
import com.wire.android.ui.registration.details.CreateAccountDataDetailViewModel
import com.wire.android.ui.registration.selector.CreateAccountSelectorNavArgs
import com.wire.android.ui.registration.selector.CreateAccountSelectorViewModel
import com.wire.android.navigation.routes.auth.AuthenticationServerLinks
import com.wire.android.navigation.routes.auth.CreateAccountRegistrationInfo
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelKey

/** Bindings safe to instantiate before any authenticated session exists. */
@BindingContainer
object AuthenticationMetroViewModelBindings {

    @Provides
    @IntoMap
    @ViewModelKey(WireActivityViewModel::class)
    fun wireActivityViewModel(viewModel: WireActivityViewModel): ViewModel = viewModel

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(AuthenticationManualViewModelFactory::class)
    @Suppress("LongParameterList")
    fun authenticationManualViewModelFactory(
        welcomeFactory: WelcomeViewModelHostFactory,
        newLoginFactory: NewLoginViewModelHostFactory,
        loginEmailFactory: LoginEmailViewModelHostFactory,
        loginSSOFactory: LoginSSOViewModelHostFactory,
        createAccountOverviewFactory: CreateAccountOverviewViewModelHostFactory,
        createAccountEmailFactory: CreateAccountEmailViewModelHostFactory,
        createAccountDetailsFactory: CreateAccountDetailsViewModelHostFactory,
        createAccountCodeFactory: CreateAccountCodeViewModelHostFactory,
        createAccountSelectorFactory: CreateAccountSelectorViewModel.Factory,
        createAccountDataDetailFactory: CreateAccountDataDetailViewModel.Factory,
        createAccountVerificationCodeFactory: CreateAccountVerificationCodeViewModel.Factory,
    ): ManualViewModelAssistedFactory = object : AuthenticationManualViewModelFactory {
        override fun welcomeViewModel(navArgs: WelcomeNavArgs): WelcomeViewModel<ServerConfig.Links> =
            welcomeFactory.create(navArgs)

        override fun newLoginViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): AppNewLoginViewModel =
            newLoginFactory.create(loginNavArgs, extras.createSavedStateHandle())

        override fun loginEmailViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): AppLoginEmailViewModel =
            loginEmailFactory.create(loginNavArgs, extras.createSavedStateHandle())

        override fun loginSSOViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): AppLoginSSOViewModel =
            loginSSOFactory.create(loginNavArgs, extras.createSavedStateHandle())

        override fun createAccountOverviewViewModel(
            customServerConfig: AuthenticationServerLinks?,
        ): CreateAccountOverviewViewModel<ServerConfig.Links> = createAccountOverviewFactory.create(customServerConfig)

        override fun createAccountEmailViewModel(
            type: CreateAccountRouteFlowType,
            customServerConfig: AuthenticationServerLinks?,
        ): CreateAccountEmailViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure> =
            createAccountEmailFactory.create(type, customServerConfig)

        override fun createAccountDetailsViewModel(
            type: CreateAccountRouteFlowType,
            customServerConfig: AuthenticationServerLinks?,
        ): CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure> =
            createAccountDetailsFactory.create(type, customServerConfig)

        override fun createAccountCodeViewModel(
            type: CreateAccountRouteFlowType,
            registrationInfo: CreateAccountRegistrationInfo,
            customServerConfig: AuthenticationServerLinks?,
        ): AppCreateAccountCodeViewModel = createAccountCodeFactory.create(type, registrationInfo, customServerConfig)

        override fun createAccountSelectorViewModel(
            navArgs: CreateAccountSelectorNavArgs,
        ): CreateAccountSelectorViewModel = createAccountSelectorFactory.create(navArgs)

        override fun createAccountDataDetailViewModel(
            navArgs: CreateAccountDataNavArgs,
        ): CreateAccountDataDetailViewModel = createAccountDataDetailFactory.create(navArgs)

        override fun createAccountVerificationCodeViewModel(
            navArgs: CreateAccountDataNavArgs,
        ): CreateAccountVerificationCodeViewModel =
            createAccountVerificationCodeFactory.create(navArgs)
    }
}
