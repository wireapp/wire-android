/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.authentication.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.KaliumCoreLogic
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
                secondFactorVerificationCode = secondFactorVerificationCode,
                modelPostfix = if (BuildConfig.PRIVATE_BUILD) " [${BuildConfig.FLAVOR}_${BuildConfig.BUILD_TYPE}]" else null
            )
        )
    }

    internal suspend fun isInitialSyncCompleted(userId: UserId): Boolean =
        userDataStoreProvider.getOrCreate(userId).initialSyncCompleted.first()

    fun updateTheApp() {
        // todo : update the app after releasing on the store
    }
}

fun AuthenticationResult.Failure.toLoginError() = when (this) {
    is AuthenticationResult.Failure.SocketError -> LoginState.Error.DialogError.ProxyError
    is AuthenticationResult.Failure.Generic -> LoginState.Error.DialogError.GenericError(this.genericFailure)
    is AuthenticationResult.Failure.InvalidCredentials -> LoginState.Error.DialogError.InvalidCredentialsError
    is AuthenticationResult.Failure.InvalidUserIdentifier -> LoginState.Error.TextFieldError.InvalidValue
}

fun RegisterClientResult.Failure.toLoginError() = when (this) {
    is RegisterClientResult.Failure.Generic -> LoginState.Error.DialogError.GenericError(this.genericFailure)
    is RegisterClientResult.Failure.InvalidCredentials -> LoginState.Error.DialogError.InvalidCredentialsError
    is RegisterClientResult.Failure.TooManyClients -> LoginState.Error.TooManyDevicesError
    is RegisterClientResult.Failure.PasswordAuthRequired -> LoginState.Error.DialogError.PasswordNeededToRegisterClient
}

fun DomainLookupUseCase.Result.Failure.toLoginError() = LoginState.Error.DialogError.GenericError(this.coreFailure)
fun AddAuthenticatedUserUseCase.Result.Failure.toLoginError(): LoginState.Error = when (this) {
    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> LoginState.Error.DialogError.GenericError(this.genericFailure)
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists -> LoginState.Error.DialogError.UserAlreadyExists
}

sealed interface PreFilledUserIdentifierType {
    data object None : PreFilledUserIdentifierType
    data class PreFilled(val userIdentifier: String) : PreFilledUserIdentifierType
}

val ServerConfig.Links.isProxyEnabled get() = this.apiProxy != null
val ServerConfig.Links.isProxyAuthRequired get() = apiProxy?.needsAuthentication ?: false
