/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.authentication.login

import androidx.annotation.VisibleForTesting
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_SSO_LOGIN_RESULT
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.data.client.ClientCapability
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@ExperimentalMaterialApi
@HiltViewModel
@Suppress("TooManyFunctions")
open class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    authServerConfigProvider: AuthServerConfigProvider,
    private val userDataStoreProvider: UserDataStoreProvider
) : ViewModel() {
    val serverConfig = authServerConfigProvider.authServer.value

    val ssoLoginResult = savedStateHandle.get<String>(EXTRA_SSO_LOGIN_RESULT)?.let {
        if (it == "null") null
        else Json.decodeFromString<DeepLinkResult.SSOLogin>(it)
    }

    var loginState by mutableStateOf(
        LoginState(
            ssoCode = TextFieldValue(savedStateHandle[SSO_CODE_SAVED_STATE_KEY] ?: String.EMPTY),
            userIdentifier = TextFieldValue(savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] ?: String.EMPTY),
            password = TextFieldValue(String.EMPTY),
            isProxyAuthRequired = if (serverConfig.apiProxy?.needsAuthentication != null)
                serverConfig.apiProxy?.needsAuthentication!! else false,
            isProxyEnabled = serverConfig.apiProxy != null
        )
    )
        protected set

    open fun updateSSOLoginError(error: LoginError) {
        loginState = if (error is LoginError.None) {
            loginState.copy(loginError = error)
        } else {
            loginState.copy(ssoLoginLoading = false, loginError = error).updateSSOLoginEnabled()
        }
    }

    open fun updateEmailLoginError(error: LoginError) {
        loginState = if (error is LoginError.None) {
            loginState.copy(loginError = error)
        } else {
            loginState.copy(emailLoginLoading = false, loginError = error).updateEmailLoginEnabled()
        }
    }

    fun onDialogDismiss() {
        clearLoginErrors()
    }

    private fun clearLoginErrors() {
        clearSSOLoginError()
        clearEmailLoginError()
    }

    fun clearSSOLoginError() {
        updateSSOLoginError(LoginError.None)
    }

    fun clearEmailLoginError() {
        updateEmailLoginError(LoginError.None)
    }

    fun onTooManyDevicesError() {
        clearLoginErrors()
        navigateToRemoveDevicesScreen()
    }

    suspend fun registerClient(
        userId: UserId,
        password: String?,
        capabilities: List<ClientCapability>? = null
    ): RegisterClientResult {
        val clientScope = clientScopeProviderFactory.create(userId).clientScope
        return clientScope.getOrRegister(
            RegisterClientUseCase.RegisterClientParam(
                password = password,
                capabilities = capabilities
            )
        )
    }

    @VisibleForTesting
    fun navigateAfterRegisterClientSuccess(userId: UserId) = viewModelScope.launch {
        if (userDataStoreProvider.getOrCreate(userId).initialSyncCompleted.first())
            navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        else
            navigationManager.navigate(NavigationCommand(NavigationItem.InitialSync.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
    }

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    private fun navigateToRemoveDevicesScreen() = viewModelScope.launch {
        navigationManager.navigate(NavigationCommand(NavigationItem.RemoveDevices.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
    }

    fun updateTheApp() {
        // todo : update the app after releasing on the store
    }

    companion object {
        const val SSO_CODE_SAVED_STATE_KEY = "sso_code"
        const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
    }
}

fun AuthenticationResult.Failure.toLoginError() = when (this) {
    is AuthenticationResult.Failure.SocketError -> LoginError.DialogError.ProxyError
    is AuthenticationResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    AuthenticationResult.Failure.InvalidCredentials -> LoginError.DialogError.InvalidCredentialsError
    AuthenticationResult.Failure.InvalidUserIdentifier -> LoginError.TextFieldError.InvalidValue
}

fun RegisterClientResult.Failure.toLoginError() = when (this) {
    is RegisterClientResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    RegisterClientResult.Failure.InvalidCredentials -> LoginError.DialogError.InvalidCredentialsError
    RegisterClientResult.Failure.TooManyClients -> LoginError.TooManyDevicesError
    RegisterClientResult.Failure.PasswordAuthRequired -> LoginError.DialogError.PasswordNeededToRegisterClient
}

fun AddAuthenticatedUserUseCase.Result.Failure.toLoginError(): LoginError = when (this) {
    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists -> LoginError.DialogError.UserAlreadyExists
}
