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
package com.wire.android.ui.registration.code

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.config.orDefault
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.android.util.WillNeverOccurError
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.register.RegisterParam
import com.wire.kalium.logic.feature.register.RegisterResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountVerificationCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val anonymousAnalyticsManager: AnonymousAnalyticsManager,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
) : ViewModel() {

    val createAccountNavArgs: CreateAccountDataNavArgs = savedStateHandle.navArgs()

    val serverConfig: ServerConfig.Links = createAccountNavArgs.customServerConfig.orDefault()

    val codeTextState: TextFieldState = TextFieldState()
    var codeState: CreateAccountVerificationCodeViewState by mutableStateOf(
        CreateAccountVerificationCodeViewState()
    )

    init {
        viewModelScope.launch {
            anonymousAnalyticsManager.sendEvent(AnalyticsEvent.RegistrationPersonalAccount.CodeVerification)
            codeTextState.textAsFlow().collectLatest {
                if (it.length == codeState.codeLength) onCodeContinue()
            }
        }
    }

    fun resendCode() {
        codeState = codeState.copy(loading = true)
        viewModelScope.launch {
            // create account does not support proxy yet
            val authScope = coreLogic.versionedAuthenticationScope(serverConfig)(null).let {
                when (it) {
                    is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

                    is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                        // TODO: show dialog
                        return@launch
                    }

                    is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                        // TODO: show dialog
                        return@launch
                    }

                    is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                        return@launch
                    }
                }
            }

            val result =
                authScope.registerScope.requestActivationCode(createAccountNavArgs.userRegistrationInfo.email)
                    .toCodeError()
            codeState = codeState.copy(loading = false, result = result)
        }
    }

    fun clearCodeError() {
        codeState = codeState.copy(result = CreateAccountCodeResult.None)
    }

    fun clearCodeField() {
        codeTextState.clearText()
    }

    @Suppress("ComplexMethod")
    private fun onCodeContinue() {
        codeState = codeState.copy(loading = true)
        viewModelScope.launch {
            // create account does not support proxy yet
            val authScope = coreLogic.versionedAuthenticationScope(serverConfig)(null).let {
                when (it) {
                    is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

                    is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                        // TODO: show dialog
                        return@launch
                    }

                    is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                        // TODO: show dialog
                        return@launch
                    }

                    is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                        return@launch
                    }
                }
            }

            val registerParam = RegisterParam.PersonalAccount(
                name = createAccountNavArgs.userRegistrationInfo.name,
                password = createAccountNavArgs.userRegistrationInfo.password,
                email = createAccountNavArgs.userRegistrationInfo.email,
                emailActivationCode = codeTextState.text.toString()
            )

            val registerResult = authScope.registerScope.register(registerParam).let {
                when (it) {
                    is RegisterResult.Failure -> {
                        anonymousAnalyticsManager.sendEvent(AnalyticsEvent.RegistrationPersonalAccount.CodeVerificationFailed)
                        updateCodeErrorState(it.toCodeError())
                        return@launch
                    }

                    is RegisterResult.Success -> it
                }
            }
            val storedUserId = addAuthenticatedUser(
                authTokens = registerResult.authData,
                ssoId = registerResult.ssoID,
                serverConfigId = registerResult.serverConfigId,
                proxyCredentials = registerResult.proxyCredentials,
                replace = false
            ).let {
                when (it) {
                    is AddAuthenticatedUserUseCase.Result.Failure -> {
                        updateCodeErrorState(it.toCodeError())
                        return@launch
                    }

                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }
            registerClient(storedUserId, registerParam)
        }
    }

    private suspend fun registerClient(
        storedUserId: UserId,
        registerParam: RegisterParam.PersonalAccount
    ) {
        registerClient(storedUserId, registerParam.password).let {
            when (it) {
                is RegisterClientResult.Failure -> {
                    updateCodeErrorState(it.toCodeError())
                }

                is RegisterClientResult.Success -> {
                    codeState =
                        codeState.copy(result = CreateAccountCodeResult.Success)
                }

                is RegisterClientResult.E2EICertificateRequired -> {
                    // TODO
                    codeState =
                        codeState.copy(result = CreateAccountCodeResult.Success)
                }
            }
        }
    }

    private fun updateCodeErrorState(codeError: CreateAccountCodeResult.Error) {
        codeState = codeState.copy(loading = false, result = codeError)
    }

    private suspend fun registerClient(userId: UserId, password: String) =
        clientScopeProviderFactory.create(userId).clientScope.getOrRegister(
            RegisterClientUseCase.RegisterClientParam(
                password = password,
                capabilities = null,
                modelPostfix = if (BuildConfig.PRIVATE_BUILD) " [${BuildConfig.FLAVOR}_${BuildConfig.BUILD_TYPE}]" else null
            )
        )

    private fun RegisterClientResult.Failure.toCodeError() = when (this) {
        is RegisterClientResult.Failure.TooManyClients -> CreateAccountCodeResult.Error.TooManyDevicesError
        is RegisterClientResult.Failure.Generic -> CreateAccountCodeResult.Error.DialogError.GenericError(
            this.genericFailure
        )

        is RegisterClientResult.Failure.InvalidCredentials ->
            throw WillNeverOccurError("RegisterClient: wrong password when register client after creating a new account")

        is RegisterClientResult.Failure.PasswordAuthRequired ->
            throw WillNeverOccurError("RegisterClient: password required to register client after creating new account with email")
    }

    private fun RegisterResult.Failure.toCodeError() = when (this) {
        RegisterResult.Failure.InvalidActivationCode -> {
            CreateAccountCodeResult.Error.TextFieldError.InvalidActivationCodeError
        }

        RegisterResult.Failure.AccountAlreadyExists -> {
            CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError
        }

        RegisterResult.Failure.BlackListed -> {
            CreateAccountCodeResult.Error.DialogError.BlackListedError
        }

        RegisterResult.Failure.EmailDomainBlocked -> {
            CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError
        }

        RegisterResult.Failure.InvalidEmail -> {
            CreateAccountCodeResult.Error.DialogError.InvalidEmailError
        }

        RegisterResult.Failure.TeamMembersLimitReached -> {
            CreateAccountCodeResult.Error.DialogError.TeamMembersLimitError
        }

        RegisterResult.Failure.UserCreationRestricted -> {
            CreateAccountCodeResult.Error.DialogError.CreationRestrictedError
        }

        is RegisterResult.Failure.Generic -> {
            CreateAccountCodeResult.Error.DialogError.GenericError(
                this.failure
            )
        }
    }

    private fun AddAuthenticatedUserUseCase.Result.Failure.toCodeError() = when (this) {
        is AddAuthenticatedUserUseCase.Result.Failure.Generic ->
            CreateAccountCodeResult.Error.DialogError.GenericError(this.genericFailure)

        AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists ->
            CreateAccountCodeResult.Error.DialogError.UserAlreadyExistsError
    }

    private fun RequestActivationCodeResult.toCodeError() = when (this) {
        RequestActivationCodeResult.Failure.AlreadyInUse -> {
            CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError
        }

        RequestActivationCodeResult.Failure.BlacklistedEmail -> {
            CreateAccountCodeResult.Error.DialogError.BlackListedError
        }

        RequestActivationCodeResult.Failure.DomainBlocked -> {
            CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError
        }

        RequestActivationCodeResult.Failure.InvalidEmail -> {
            CreateAccountCodeResult.Error.DialogError.InvalidEmailError
        }

        is RequestActivationCodeResult.Failure.Generic -> {
            CreateAccountCodeResult.Error.DialogError.GenericError(
                this.failure
            )
        }

        RequestActivationCodeResult.Success -> {
            CreateAccountCodeResult.None
        }
    }
}
