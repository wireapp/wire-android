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
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewNavArgs
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModel
import com.wire.android.ui.authentication.welcome.WelcomeNavArgs
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.newauthentication.login.NewLoginViewModel
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeViewModel
import com.wire.android.ui.registration.details.CreateAccountDataDetailViewModel
import com.wire.android.ui.registration.selector.CreateAccountSelectorNavArgs
import com.wire.android.ui.registration.selector.CreateAccountSelectorViewModel
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
        welcomeFactory: WelcomeViewModel.Factory,
        newLoginFactory: NewLoginViewModel.Factory,
        loginEmailFactory: LoginEmailViewModel.Factory,
        loginSSOFactory: LoginSSOViewModel.Factory,
        createAccountOverviewFactory: CreateAccountOverviewViewModel.Factory,
        createAccountEmailFactory: CreateAccountEmailViewModel.Factory,
        createAccountDetailsFactory: CreateAccountDetailsViewModel.Factory,
        createAccountCodeFactory: CreateAccountCodeViewModel.Factory,
        createAccountSelectorFactory: CreateAccountSelectorViewModel.Factory,
        createAccountDataDetailFactory: CreateAccountDataDetailViewModel.Factory,
        createAccountVerificationCodeFactory: CreateAccountVerificationCodeViewModel.Factory,
    ): ManualViewModelAssistedFactory = object : AuthenticationManualViewModelFactory {
        override fun welcomeViewModel(navArgs: WelcomeNavArgs): WelcomeViewModel =
            welcomeFactory.create(navArgs)

        override fun newLoginViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): NewLoginViewModel =
            newLoginFactory.create(loginNavArgs, extras.createSavedStateHandle())

        override fun loginEmailViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): LoginEmailViewModel =
            loginEmailFactory.create(loginNavArgs, extras.createSavedStateHandle())

        override fun loginSSOViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): LoginSSOViewModel =
            loginSSOFactory.create(loginNavArgs, extras.createSavedStateHandle())

        override fun createAccountOverviewViewModel(
            navArgs: CreateAccountOverviewNavArgs,
        ): CreateAccountOverviewViewModel = createAccountOverviewFactory.create(navArgs)

        override fun createAccountEmailViewModel(navArgs: CreateAccountNavArgs): CreateAccountEmailViewModel =
            createAccountEmailFactory.create(navArgs)

        override fun createAccountDetailsViewModel(navArgs: CreateAccountNavArgs): CreateAccountDetailsViewModel =
            createAccountDetailsFactory.create(navArgs)

        override fun createAccountCodeViewModel(navArgs: CreateAccountNavArgs): CreateAccountCodeViewModel =
            createAccountCodeFactory.create(navArgs)

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
