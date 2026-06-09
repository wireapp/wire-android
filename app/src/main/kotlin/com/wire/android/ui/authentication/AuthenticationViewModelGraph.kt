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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.metro.LocalWireViewModelScopeKey
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.scopedMetroViewModelKey
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
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel as metroxViewModel

interface AuthenticationViewModelGraph : MetroViewModelGraph

interface AuthenticationManualViewModelFactory : ManualViewModelAssistedFactory {
    fun loginEmailViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): LoginEmailViewModel
    fun loginSSOViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): LoginSSOViewModel
    fun clearSessionViewModel(cancelUserId: UserId?): ClearSessionViewModel
}

val LocalAuthenticationCancelUserId = staticCompositionLocalOf<UserId?> {
    null
}

@Composable
inline fun <reified VM> authenticationViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    key: String? = null,
): VM where VM : ViewModel =
    metroxViewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        key = authenticationViewModelKey<VM>(viewModelStoreOwner, key),
    )

@Composable
fun welcomeViewModel(): WelcomeViewModel =
    authenticationViewModel()

@Composable
fun newLoginViewModel(): NewLoginViewModel =
    authenticationViewModel()

@Composable
fun loginEmailViewModel(
    loginNavArgs: LoginNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): LoginEmailViewModel =
    assistedMetroViewModel<LoginEmailViewModel, AuthenticationManualViewModelFactory>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = authenticationViewModelKey<LoginEmailViewModel>(viewModelStoreOwner, loginNavArgs.toString()),
    ) { extras ->
        loginEmailViewModel(loginNavArgs, extras)
    }

@Composable
fun loginSSOViewModel(
    loginNavArgs: LoginNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): LoginSSOViewModel =
    assistedMetroViewModel<LoginSSOViewModel, AuthenticationManualViewModelFactory>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = authenticationViewModelKey<LoginSSOViewModel>(viewModelStoreOwner, loginNavArgs.toString()),
    ) { extras ->
        loginSSOViewModel(loginNavArgs, extras)
    }

@Composable
fun registerDeviceViewModel(): RegisterDeviceViewModel =
    authenticationViewModel()

@Composable
fun removeDeviceViewModel(): RemoveDeviceViewModel =
    authenticationViewModel()

@Composable
fun clearSessionViewModel(): ClearSessionViewModel {
    val cancelUserId = LocalAuthenticationCancelUserId.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    return assistedMetroViewModel<ClearSessionViewModel, AuthenticationManualViewModelFactory>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = authenticationViewModelKey<ClearSessionViewModel>(viewModelStoreOwner, cancelUserId?.toString()),
    ) {
        clearSessionViewModel(cancelUserId)
    }
}

@Composable
fun createAccountUsernameViewModel(): CreateAccountUsernameViewModel =
    authenticationViewModel()

@Composable
fun createAccountOverviewViewModel(): CreateAccountOverviewViewModel =
    authenticationViewModel()

@Composable
fun createAccountEmailViewModel(): CreateAccountEmailViewModel =
    authenticationViewModel()

@Composable
fun createAccountDetailsViewModel(): CreateAccountDetailsViewModel =
    authenticationViewModel()

@Composable
fun createAccountCodeViewModel(): CreateAccountCodeViewModel =
    authenticationViewModel()

@Composable
fun createAccountSummaryViewModel(): CreateAccountSummaryViewModel =
    authenticationViewModel()

@Composable
fun createAccountSelectorViewModel(): CreateAccountSelectorViewModel =
    authenticationViewModel()

@Composable
fun createAccountDataDetailViewModel(): CreateAccountDataDetailViewModel =
    authenticationViewModel()

@Composable
fun createAccountVerificationCodeViewModel(): CreateAccountVerificationCodeViewModel =
    authenticationViewModel()

@Composable
inline fun <reified VM : ViewModel> authenticationViewModelKey(
    viewModelStoreOwner: ViewModelStoreOwner,
    key: String? = null,
): String? =
    scopedMetroViewModelKey(
        defaultKey = VM::class.qualifiedName,
        key = listOfNotNull(key, viewModelStoreOwner.hashCode().toString()).joinToString(":"),
        scopeKey = LocalWireViewModelScopeKey.current,
    )
