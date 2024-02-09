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
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.client.ClientCapability
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.DomainLookupUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
open class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    protected val authServerConfigProvider: AuthServerConfigProvider,
    private val userDataStoreProvider: UserDataStoreProvider,
    @KaliumCoreLogic protected val coreLogic: CoreLogic
) : ViewModel() {
    var serverConfig: ServerConfig.Links by mutableStateOf(authServerConfigProvider.authServer.value)
        private set

    init {
        viewModelScope.launch {
            authServerConfigProvider.authServer.collect {
                serverConfig = it
            }
        }
    }

    private val loginNavArgs: LoginNavArgs = savedStateHandle.navArgs()
    private val preFilledUserIdentifier: PreFilledUserIdentifierType = loginNavArgs.userHandle.let {
        if (it.isNullOrEmpty()) PreFilledUserIdentifierType.None else PreFilledUserIdentifierType.PreFilled(it)
    }

    var loginState by mutableStateOf(
        LoginState(
            userInput = TextFieldValue(savedStateHandle[SSO_CODE_SAVED_STATE_KEY] ?: String.EMPTY),
            userIdentifier = TextFieldValue(
                if (preFilledUserIdentifier is PreFilledUserIdentifierType.PreFilled) preFilledUserIdentifier.userIdentifier
                else savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] ?: String.EMPTY
            ),
            userIdentifierEnabled = preFilledUserIdentifier is PreFilledUserIdentifierType.None,
            password = TextFieldValue(String.EMPTY),
            isProxyAuthRequired =
            if (serverConfig.apiProxy?.needsAuthentication != null) serverConfig.apiProxy?.needsAuthentication!!
            else false,
            isProxyEnabled = serverConfig.apiProxy != null
        )
    )
        @VisibleForTesting
        set

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

    fun clearLoginErrors() {
        clearSSOLoginError()
        clearEmailLoginError()
    }

    fun clearSSOLoginError() {
        updateSSOLoginError(LoginError.None)
    }

    fun clearEmailLoginError() {
        updateEmailLoginError(LoginError.None)
    }

    suspend fun registerClient(
        userId: UserId,
        password: String?,
        secondFactorVerificationCode: String? = null,
        capabilities: List<ClientCapability>? = null,
    ): RegisterClientResult {
        val clientScope = clientScopeProviderFactory.create(userId).clientScope
        return clientScope.getOrRegister(
            RegisterClientUseCase.RegisterClientParam(
                password = password,
                capabilities = capabilities,
                secondFactorVerificationCode = secondFactorVerificationCode
            )
        )
    }

    internal suspend fun isInitialSyncCompleted(userId: UserId): Boolean =
        userDataStoreProvider.getOrCreate(userId).initialSyncCompleted.first()

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
    is AuthenticationResult.Failure.InvalidCredentials -> LoginError.DialogError.InvalidCredentialsError
    is AuthenticationResult.Failure.InvalidUserIdentifier -> LoginError.TextFieldError.InvalidValue
}

fun RegisterClientResult.Failure.toLoginError() = when (this) {
    is RegisterClientResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    is RegisterClientResult.Failure.InvalidCredentials -> LoginError.DialogError.InvalidCredentialsError
    is RegisterClientResult.Failure.TooManyClients -> LoginError.TooManyDevicesError
    is RegisterClientResult.Failure.PasswordAuthRequired -> LoginError.DialogError.PasswordNeededToRegisterClient
}

fun DomainLookupUseCase.Result.Failure.toLoginError() = LoginError.DialogError.GenericError(this.coreFailure)
fun AddAuthenticatedUserUseCase.Result.Failure.toLoginError(): LoginError = when (this) {
    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists -> LoginError.DialogError.UserAlreadyExists
}

sealed interface PreFilledUserIdentifierType {
    object None : PreFilledUserIdentifierType
    data class PreFilled(val userIdentifier: String) : PreFilledUserIdentifierType
}
