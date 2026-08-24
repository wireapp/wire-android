/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.ui.authentication.create.code.AppCreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
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

@Composable
fun createAccountOverviewViewModel(
    customServerConfig: AuthenticationServerLinks?,
    viewModelStoreOwner: ViewModelStoreOwner
): CreateAccountOverviewViewModel<ServerConfig.Links> =
    wireAssistedMetroViewModel<
        CreateAccountOverviewViewModel<ServerConfig.Links>,
        AuthenticationManualViewModelFactory
        >(owner = viewModelStoreOwner) { createAccountOverviewViewModel(customServerConfig) }

@Composable
fun createAccountEmailViewModel(
    type: CreateAccountRouteFlowType,
    customServerConfig: AuthenticationServerLinks?,
    viewModelStoreOwner: ViewModelStoreOwner
): CreateAccountEmailViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure> =
    wireAssistedMetroViewModel<
        CreateAccountEmailViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure>,
        AuthenticationManualViewModelFactory
        >(owner = viewModelStoreOwner) { createAccountEmailViewModel(type, customServerConfig) }

@Composable
fun createAccountDetailsViewModel(
    type: CreateAccountRouteFlowType,
    customServerConfig: AuthenticationServerLinks?,
    viewModelStoreOwner: ViewModelStoreOwner
): CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure> =
    wireAssistedMetroViewModel<
        CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure>,
        AuthenticationManualViewModelFactory
        >(owner = viewModelStoreOwner) { createAccountDetailsViewModel(type, customServerConfig) }

@Composable
fun createAccountCodeViewModel(
    type: CreateAccountRouteFlowType,
    registrationInfo: CreateAccountRegistrationInfo,
    customServerConfig: AuthenticationServerLinks?,
    viewModelStoreOwner: ViewModelStoreOwner
): AppCreateAccountCodeViewModel =
    wireAssistedMetroViewModel<
        AppCreateAccountCodeViewModel,
        AuthenticationManualViewModelFactory
        >(owner = viewModelStoreOwner) {
            createAccountCodeViewModel(
            type,
            registrationInfo,
            customServerConfig
        )
        }

@Composable
fun createAccountSelectorViewModel(
    navArgs: CreateAccountSelectorNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner
): CreateAccountSelectorViewModel =
    wireAssistedMetroViewModel<
        CreateAccountSelectorViewModel,
        AuthenticationManualViewModelFactory
        >(owner = viewModelStoreOwner) { createAccountSelectorViewModel(navArgs) }

@Composable
fun createAccountDataDetailViewModel(
    navArgs: CreateAccountDataNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner
): CreateAccountDataDetailViewModel =
    wireAssistedMetroViewModel<
        CreateAccountDataDetailViewModel,
        AuthenticationManualViewModelFactory
        >(owner = viewModelStoreOwner) { createAccountDataDetailViewModel(navArgs) }

@Composable
fun createAccountVerificationCodeViewModel(
    navArgs: CreateAccountDataNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner
): CreateAccountVerificationCodeViewModel =
    wireAssistedMetroViewModel<
        CreateAccountVerificationCodeViewModel,
        AuthenticationManualViewModelFactory
        >(owner = viewModelStoreOwner) { createAccountVerificationCodeViewModel(navArgs) }
