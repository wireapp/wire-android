/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.authentication

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import com.wire.android.ui.authentication.create.code.AppCreateAccountCodeViewModel
import com.wire.android.ui.authentication.create.details.CreateAccountDetailsViewModel
import com.wire.android.ui.authentication.create.email.CreateAccountEmailViewModel
import com.wire.android.ui.authentication.create.overview.CreateAccountOverviewViewModel
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameViewModel
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeViewModel
import com.wire.android.ui.registration.details.CreateAccountDataDetailViewModel
import com.wire.android.ui.registration.selector.CreateAccountSelectorViewModel
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable fun createAccountUsernameViewModel(): CreateAccountUsernameViewModel<CoreFailure> = authenticationViewModel()

@Composable fun createAccountOverviewViewModel(): CreateAccountOverviewViewModel<ServerConfig.Links> = authenticationViewModel()

@Composable
fun createAccountEmailViewModel(): CreateAccountEmailViewModel<
        com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType,
        ServerConfig.Links,
        CoreFailure,
    > = authenticationViewModel()

@Composable fun createAccountDetailsViewModel(): CreateAccountDetailsViewModel<
    ServerConfig.Links,
    NetworkFailure
    > = authenticationViewModel()

@Composable fun createAccountCodeViewModel(): AppCreateAccountCodeViewModel = authenticationViewModel()

@Composable fun createAccountSelectorViewModel(): CreateAccountSelectorViewModel = authenticationViewModel()

@Composable fun createAccountDataDetailViewModel(): CreateAccountDataDetailViewModel = authenticationViewModel()

@Composable fun createAccountVerificationCodeViewModel(): CreateAccountVerificationCodeViewModel = authenticationViewModel()

@Composable fun createAccountUsernameViewModel(viewModelStoreOwner: ViewModelStoreOwner):
    CreateAccountUsernameViewModel<CoreFailure> = authenticationViewModel(viewModelStoreOwner)
