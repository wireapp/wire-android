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
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.di.metro.wireMetroViewModel
import com.wire.android.ui.authentication.create.code.AppCreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameViewModel
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.register.RegisterDeviceViewModel
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceViewModel
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.AppLoginSSOViewModel
import com.wire.android.ui.authentication.welcome.WelcomeNavArgs
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.newauthentication.login.AppNewLoginViewModel
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeViewModel
import com.wire.android.ui.registration.details.CreateAccountDataDetailViewModel
import com.wire.android.ui.registration.selector.CreateAccountSelectorViewModel
import com.wire.android.ui.registration.selector.CreateAccountSelectorNavArgs
import com.wire.android.navigation.routes.auth.AuthenticationServerLinks
import com.wire.android.navigation.routes.auth.CreateAccountRegistrationInfo
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

interface AuthenticationViewModelGraph : MetroViewModelGraph

interface AuthenticationManualViewModelFactory : ManualViewModelAssistedFactory {
    fun welcomeViewModel(navArgs: WelcomeNavArgs): WelcomeViewModel<ServerConfig.Links>
    fun newLoginViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): AppNewLoginViewModel
    fun loginEmailViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): AppLoginEmailViewModel
    fun loginSSOViewModel(loginNavArgs: LoginNavArgs, extras: CreationExtras): AppLoginSSOViewModel
    fun createAccountOverviewViewModel(customServerConfig: AuthenticationServerLinks?): CreateAccountOverviewViewModel<ServerConfig.Links>
    fun createAccountEmailViewModel(
        type: CreateAccountRouteFlowType,
        customServerConfig: AuthenticationServerLinks?,
    ): CreateAccountEmailViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure>
    fun createAccountDetailsViewModel(
        type: CreateAccountRouteFlowType,
        customServerConfig: AuthenticationServerLinks?,
    ): CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure>
    fun createAccountCodeViewModel(
        type: CreateAccountRouteFlowType,
        registrationInfo: CreateAccountRegistrationInfo,
        customServerConfig: AuthenticationServerLinks?,
    ): AppCreateAccountCodeViewModel
    fun createAccountSelectorViewModel(navArgs: CreateAccountSelectorNavArgs): CreateAccountSelectorViewModel
    fun createAccountDataDetailViewModel(navArgs: CreateAccountDataNavArgs): CreateAccountDataDetailViewModel
    fun createAccountVerificationCodeViewModel(
        navArgs: CreateAccountDataNavArgs,
    ): CreateAccountVerificationCodeViewModel
}

interface SessionAuthenticationManualViewModelFactory : ManualViewModelAssistedFactory {
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
): VM where VM : ViewModel =
    wireMetroViewModel(
        owner = viewModelStoreOwner,
    )

@Composable
fun welcomeViewModel(): WelcomeViewModel<ServerConfig.Links> =
    authenticationViewModel()

@Composable
fun welcomeViewModel(
    navArgs: WelcomeNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): WelcomeViewModel<ServerConfig.Links> =
    wireAssistedMetroViewModel<WelcomeViewModel<ServerConfig.Links>, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) {
        welcomeViewModel(navArgs)
    }

@Composable
fun newLoginViewModel(): AppNewLoginViewModel =
    authenticationViewModel()

@Composable
fun newLoginViewModel(
    loginNavArgs: LoginNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): AppNewLoginViewModel =
    wireAssistedMetroViewModel<AppNewLoginViewModel, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) { extras ->
        newLoginViewModel(loginNavArgs, extras)
    }

@Composable
fun loginEmailViewModel(
    loginNavArgs: LoginNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): AppLoginEmailViewModel =
    wireAssistedMetroViewModel<AppLoginEmailViewModel, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) { extras ->
        loginEmailViewModel(loginNavArgs, extras)
    }

@Composable
fun loginSSOViewModel(
    loginNavArgs: LoginNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): AppLoginSSOViewModel =
    wireAssistedMetroViewModel<AppLoginSSOViewModel, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) { extras ->
        loginSSOViewModel(loginNavArgs, extras)
    }

@Composable
fun registerDeviceViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
): RegisterDeviceViewModel<com.wire.navigation.WireSessionId> =
    authenticationViewModel(viewModelStoreOwner)

@Composable
fun removeDeviceViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
): RemoveDeviceViewModel<Device> =
    authenticationViewModel(viewModelStoreOwner)

@Composable
fun clearSessionViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
): ClearSessionViewModel {
    val cancelUserId = LocalAuthenticationCancelUserId.current
    return wireAssistedMetroViewModel<ClearSessionViewModel, SessionAuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) {
        clearSessionViewModel(cancelUserId)
    }
}

@Composable
fun createAccountUsernameViewModel(): CreateAccountUsernameViewModel<CoreFailure> =
    authenticationViewModel()

@Composable
fun createAccountOverviewViewModel(): CreateAccountOverviewViewModel<ServerConfig.Links> =
    authenticationViewModel()

@Composable
fun createAccountEmailViewModel(): CreateAccountEmailViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure> =
    authenticationViewModel()

@Composable
fun createAccountDetailsViewModel(): CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure> =
    authenticationViewModel()

@Composable
fun createAccountCodeViewModel(): AppCreateAccountCodeViewModel =
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
fun createAccountUsernameViewModel(
    viewModelStoreOwner: ViewModelStoreOwner,
): CreateAccountUsernameViewModel<CoreFailure> =
    authenticationViewModel(viewModelStoreOwner = viewModelStoreOwner)

@Composable
fun createAccountOverviewViewModel(
    customServerConfig: AuthenticationServerLinks?,
    viewModelStoreOwner: ViewModelStoreOwner,
): CreateAccountOverviewViewModel<ServerConfig.Links> =
    wireAssistedMetroViewModel<CreateAccountOverviewViewModel<ServerConfig.Links>, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) {
        createAccountOverviewViewModel(customServerConfig)
    }

@Composable
fun createAccountEmailViewModel(
    type: CreateAccountRouteFlowType,
    customServerConfig: AuthenticationServerLinks?,
    viewModelStoreOwner: ViewModelStoreOwner,
): CreateAccountEmailViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure> =
    wireAssistedMetroViewModel<CreateAccountEmailViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure>, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) {
        createAccountEmailViewModel(type, customServerConfig)
    }

@Composable
fun createAccountDetailsViewModel(
    type: CreateAccountRouteFlowType,
    customServerConfig: AuthenticationServerLinks?,
    viewModelStoreOwner: ViewModelStoreOwner,
): CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure> =
    wireAssistedMetroViewModel<CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure>, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) {
        createAccountDetailsViewModel(type, customServerConfig)
    }

@Composable
fun createAccountCodeViewModel(
    type: CreateAccountRouteFlowType,
    registrationInfo: CreateAccountRegistrationInfo,
    customServerConfig: AuthenticationServerLinks?,
    viewModelStoreOwner: ViewModelStoreOwner,
): AppCreateAccountCodeViewModel =
    wireAssistedMetroViewModel<AppCreateAccountCodeViewModel, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) {
        createAccountCodeViewModel(type, registrationInfo, customServerConfig)
    }

@Composable
fun createAccountSelectorViewModel(
    navArgs: CreateAccountSelectorNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner,
): CreateAccountSelectorViewModel =
    wireAssistedMetroViewModel<CreateAccountSelectorViewModel, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) {
        createAccountSelectorViewModel(navArgs)
    }

@Composable
fun createAccountDataDetailViewModel(
    navArgs: CreateAccountDataNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner,
): CreateAccountDataDetailViewModel =
    wireAssistedMetroViewModel<CreateAccountDataDetailViewModel, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) {
        createAccountDataDetailViewModel(navArgs)
    }

@Composable
fun createAccountVerificationCodeViewModel(
    navArgs: CreateAccountDataNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner,
): CreateAccountVerificationCodeViewModel =
    wireAssistedMetroViewModel<CreateAccountVerificationCodeViewModel, AuthenticationManualViewModelFactory>(
        owner = viewModelStoreOwner,
    ) {
        createAccountVerificationCodeViewModel(navArgs)
    }
