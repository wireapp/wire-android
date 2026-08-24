/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.di.metro.wireMetroViewModel
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.email.AppLoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.AppLoginSSOViewModel
import com.wire.android.ui.authentication.welcome.WelcomeNavArgs
import com.wire.android.ui.authentication.welcome.WelcomeViewModel
import com.wire.android.ui.newauthentication.login.AppNewLoginViewModel
import com.wire.kalium.logic.configuration.server.ServerConfig

internal val authenticationViewModelStoreOwner: ViewModelStoreOwner
    @Composable get() = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }

@Composable
inline fun <reified VM> authenticationViewModel(viewModelStoreOwner: ViewModelStoreOwner =
    authenticationViewModelStoreOwner): VM where VM : ViewModel =
    wireMetroViewModel(owner = viewModelStoreOwner)

@Composable
fun welcomeViewModel(): WelcomeViewModel<ServerConfig.Links> = authenticationViewModel()

@Composable
fun welcomeViewModel(navArgs: WelcomeNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = authenticationViewModelStoreOwner): WelcomeViewModel<ServerConfig.Links> =
    wireAssistedMetroViewModel<WelcomeViewModel<ServerConfig.Links>, AuthenticationManualViewModelFactory>(owner = viewModelStoreOwner) {
        welcomeViewModel(navArgs)
    }

@Composable
fun newLoginViewModel(): AppNewLoginViewModel = authenticationViewModel()

@Composable
fun newLoginViewModel(loginNavArgs: LoginNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = authenticationViewModelStoreOwner): AppNewLoginViewModel =
    wireAssistedMetroViewModel<AppNewLoginViewModel, AuthenticationManualViewModelFactory>(owner = viewModelStoreOwner) { extras ->
        newLoginViewModel(loginNavArgs, extras)
    }

@Composable
fun loginEmailViewModel(loginNavArgs: LoginNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = authenticationViewModelStoreOwner): AppLoginEmailViewModel =
    wireAssistedMetroViewModel<AppLoginEmailViewModel, AuthenticationManualViewModelFactory>(owner = viewModelStoreOwner) { extras ->
        loginEmailViewModel(loginNavArgs, extras)
    }

@Composable
fun loginSSOViewModel(loginNavArgs: LoginNavArgs,
    viewModelStoreOwner: ViewModelStoreOwner = authenticationViewModelStoreOwner): AppLoginSSOViewModel =
    wireAssistedMetroViewModel<AppLoginSSOViewModel, AuthenticationManualViewModelFactory>(owner = viewModelStoreOwner) { extras ->
        loginSSOViewModel(loginNavArgs, extras)
    }
