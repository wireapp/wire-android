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
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.authentication

import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.metroSavedStateViewModel
import com.wire.android.di.metro.metroViewModel
import com.wire.android.ui.authentication.create.code.CreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryViewModel
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameViewModel
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.register.RegisterDeviceViewModel
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceViewModel
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModel
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.newauthentication.login.NewLoginViewModel
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeViewModel
import com.wire.android.ui.registration.details.CreateAccountDataDetailViewModel
import com.wire.android.ui.registration.selector.CreateAccountSelectorViewModel

interface AuthenticationViewModelGraph : MetroViewModelGraph {
    val authenticationViewModelFactory: AuthenticationViewModelFactory
}

@Composable
inline fun <reified VM> authenticationViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    crossinline create: AuthenticationViewModelFactory.() -> VM,
): VM where VM : ViewModel =
    metroViewModel<AuthenticationViewModelGraph, VM>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
    ) {
        authenticationViewModelFactory.create()
    }

@Composable
inline fun <reified VM> authenticationSavedStateViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
    crossinline create: AuthenticationViewModelFactory.(SavedStateHandle) -> VM,
): VM where VM : ViewModel =
    metroSavedStateViewModel<AuthenticationViewModelGraph, VM>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
    ) { savedStateHandle ->
        authenticationViewModelFactory.create(savedStateHandle)
    }

@Composable
fun welcomeViewModel(): WelcomeViewModel = authenticationSavedStateViewModel { welcomeViewModel(it) }

@Composable
fun newLoginViewModel(): NewLoginViewModel = authenticationSavedStateViewModel { newLoginViewModel(it) }

@Composable
fun loginEmailViewModel(
    loginNavArgs: LoginNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): LoginEmailViewModel =
    authenticationSavedStateViewModel(viewModelStoreOwner = viewModelStoreOwner) {
        loginEmailViewModel(loginNavArgs, it)
    }

@Composable
fun loginSSOViewModel(loginNavArgs: LoginNavArgs): LoginSSOViewModel =
    authenticationSavedStateViewModel { loginSSOViewModel(loginNavArgs, it) }

@Composable
fun registerDeviceViewModel(): RegisterDeviceViewModel = authenticationViewModel { registerDeviceViewModel() }

@Composable
fun removeDeviceViewModel(): RemoveDeviceViewModel = authenticationViewModel { removeDeviceViewModel() }

@Composable
fun clearSessionViewModel(): ClearSessionViewModel = authenticationViewModel { clearSessionViewModel() }

@Composable
fun createAccountUsernameViewModel(): CreateAccountUsernameViewModel =
    authenticationViewModel { createAccountUsernameViewModel() }

@Composable
fun createAccountOverviewViewModel(): CreateAccountOverviewViewModel =
    authenticationSavedStateViewModel { createAccountOverviewViewModel(it) }

@Composable
fun createAccountEmailViewModel(): CreateAccountEmailViewModel =
    authenticationSavedStateViewModel { createAccountEmailViewModel(it) }

@Composable
fun createAccountDetailsViewModel(): CreateAccountDetailsViewModel =
    authenticationSavedStateViewModel { createAccountDetailsViewModel(it) }

@Composable
fun createAccountCodeViewModel(): CreateAccountCodeViewModel =
    authenticationSavedStateViewModel { createAccountCodeViewModel(it) }

@Composable
fun createAccountSummaryViewModel(): CreateAccountSummaryViewModel =
    authenticationSavedStateViewModel { createAccountSummaryViewModel(it) }

@Composable
fun createAccountSelectorViewModel(): CreateAccountSelectorViewModel =
    authenticationSavedStateViewModel { createAccountSelectorViewModel(it) }

@Composable
fun createAccountDataDetailViewModel(): CreateAccountDataDetailViewModel =
    authenticationSavedStateViewModel { createAccountDataDetailViewModel(it) }

@Composable
fun createAccountVerificationCodeViewModel(): CreateAccountVerificationCodeViewModel =
    authenticationSavedStateViewModel { createAccountVerificationCodeViewModel(it) }
