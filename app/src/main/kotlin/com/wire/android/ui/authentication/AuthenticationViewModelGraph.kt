/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.CreationExtras
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.ui.authentication.create.code.AppCreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.AppLoginSSOViewModel
import com.wire.android.ui.authentication.welcome.WelcomeNavArgs
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.newauthentication.login.AppNewLoginViewModel
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
    fun createAccountVerificationCodeViewModel(navArgs: CreateAccountDataNavArgs): CreateAccountVerificationCodeViewModel
}

interface SessionAuthenticationManualViewModelFactory : ManualViewModelAssistedFactory {
    fun clearSessionViewModel(cancelUserId: UserId?): ClearSessionViewModel
}

val LocalAuthenticationCancelUserId = staticCompositionLocalOf<UserId?> { null }
