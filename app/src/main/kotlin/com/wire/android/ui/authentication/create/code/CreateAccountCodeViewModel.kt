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
package com.wire.android.ui.authentication.create.code

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.common.textfield.CodeFieldValue
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
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Cover this viewModel  with unit test
@HiltViewModel
class CreateAccountCodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    authServerConfigProvider: AuthServerConfigProvider
) : ViewModel() {

    val createAccountNavArgs: CreateAccountNavArgs = savedStateHandle.navArgs()

    val serverConfig: ServerConfig.Links = authServerConfigProvider.authServer.value

    var codeState: CreateAccountCodeViewState by mutableStateOf(CreateAccountCodeViewState(createAccountNavArgs.flowType))

    fun onCodeChange(newValue: CodeFieldValue, onSuccess: () -> Unit) {
        codeState = codeState.copy(code = newValue, error = CreateAccountCodeViewState.CodeError.None)
        if (newValue.isFullyFilled) onCodeContinue(onSuccess)
    }

    fun resendCode() {
        codeState = codeState.copy(loading = true)
        viewModelScope.launch {
            val authScope = coreLogic.versionedAuthenticationScope(serverConfig)().let {
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

            val codeError = authScope.registerScope.requestActivationCode(createAccountNavArgs.userRegistrationInfo.email).toCodeError()
            codeState = codeState.copy(loading = false, error = codeError)
        }
    }

    fun clearCodeError() {
        codeState = codeState.copy(error = CreateAccountCodeViewState.CodeError.None)
    }

    fun clearCodeField() {
        codeState = codeState.copy(code = CodeFieldValue(text = TextFieldValue(""), isFullyFilled = false))
    }

    private fun registerParamFromType() = when (createAccountNavArgs.flowType) {
        CreateAccountFlowType.CreatePersonalAccount ->
            RegisterParam.PrivateAccount(
                firstName = createAccountNavArgs.userRegistrationInfo.firstName,
                lastName = createAccountNavArgs.userRegistrationInfo.lastName,
                password = createAccountNavArgs.userRegistrationInfo.password,
                email = createAccountNavArgs.userRegistrationInfo.email,
                emailActivationCode = codeState.code.text.text
            )

        CreateAccountFlowType.CreateTeam ->
            RegisterParam.Team(
                firstName = createAccountNavArgs.userRegistrationInfo.firstName,
                lastName = createAccountNavArgs.userRegistrationInfo.lastName,
                password = createAccountNavArgs.userRegistrationInfo.password,
                email = createAccountNavArgs.userRegistrationInfo.email,
                emailActivationCode = codeState.code.text.text,
                teamName = createAccountNavArgs.userRegistrationInfo.teamName,
                teamIcon = "default"
            )
    }

    @Suppress("ComplexMethod")
    private fun onCodeContinue(onSuccess: () -> Unit) {
        codeState = codeState.copy(loading = true)
        viewModelScope.launch {
            val authScope = coreLogic.versionedAuthenticationScope(serverConfig)().let {
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

            val registerParam = registerParamFromType()

            val registerResult = authScope.registerScope.register(registerParam).let {
                when (it) {
                    is RegisterResult.Failure -> {
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
            registerClient(storedUserId, registerParam.password).let {
                when (it) {
                    is RegisterClientResult.Failure -> {
                        updateCodeErrorState(it.toCodeError())
                        return@launch
                    }

                    is RegisterClientResult.Success -> {
                        onSuccess()
                    }
                    is RegisterClientResult.E2EICertificateRequired ->{
                        //todo
                        onSuccess()
                    }
                }
            }
        }
    }

    private fun updateCodeErrorState(codeError: CreateAccountCodeViewState.CodeError) {
        codeState = if (codeError is CreateAccountCodeViewState.CodeError.None) {
            codeState.copy(error = codeError)
        } else {
            codeState.copy(loading = false, error = codeError)
        }
    }

    private suspend fun registerClient(userId: UserId, password: String) =
        clientScopeProviderFactory.create(userId).clientScope.getOrRegister(
            RegisterClientUseCase.RegisterClientParam(
                password = password,
                capabilities = null
            )
        )

    private fun RegisterClientResult.Failure.toCodeError() = when (this) {
        is RegisterClientResult.Failure.TooManyClients -> CreateAccountCodeViewState.CodeError.TooManyDevicesError
        is RegisterClientResult.Failure.Generic -> CreateAccountCodeViewState.CodeError.DialogError.GenericError(this.genericFailure)
        is RegisterClientResult.Failure.InvalidCredentials ->
            throw WillNeverOccurError("RegisterClient: wrong password when register client after creating a new account")

        is RegisterClientResult.Failure.PasswordAuthRequired ->
            throw WillNeverOccurError("RegisterClient: password required to register client after creating new account with email")
    }

    private fun RegisterResult.Failure.toCodeError() = when (this) {
        RegisterResult.Failure.InvalidActivationCode -> CreateAccountCodeViewState.CodeError.TextFieldError.InvalidActivationCodeError
        RegisterResult.Failure.AccountAlreadyExists -> CreateAccountCodeViewState.CodeError.DialogError.AccountAlreadyExistsError
        RegisterResult.Failure.BlackListed -> CreateAccountCodeViewState.CodeError.DialogError.BlackListedError
        RegisterResult.Failure.EmailDomainBlocked -> CreateAccountCodeViewState.CodeError.DialogError.EmailDomainBlockedError
        RegisterResult.Failure.InvalidEmail -> CreateAccountCodeViewState.CodeError.DialogError.InvalidEmailError
        RegisterResult.Failure.TeamMembersLimitReached -> CreateAccountCodeViewState.CodeError.DialogError.TeamMembersLimitError
        RegisterResult.Failure.UserCreationRestricted -> CreateAccountCodeViewState.CodeError.DialogError.CreationRestrictedError
        is RegisterResult.Failure.Generic -> CreateAccountCodeViewState.CodeError.DialogError.GenericError(this.failure)
    }

    private fun AddAuthenticatedUserUseCase.Result.Failure.toCodeError() = when (this) {
        is AddAuthenticatedUserUseCase.Result.Failure.Generic ->
            CreateAccountCodeViewState.CodeError.DialogError.GenericError(this.genericFailure)

        AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists -> CreateAccountCodeViewState.CodeError.DialogError.UserAlreadyExists
    }

    private fun RequestActivationCodeResult.toCodeError() = when (this) {
        RequestActivationCodeResult.Failure.AlreadyInUse -> CreateAccountCodeViewState.CodeError.DialogError.AccountAlreadyExistsError
        RequestActivationCodeResult.Failure.BlacklistedEmail -> CreateAccountCodeViewState.CodeError.DialogError.BlackListedError
        RequestActivationCodeResult.Failure.DomainBlocked -> CreateAccountCodeViewState.CodeError.DialogError.EmailDomainBlockedError
        RequestActivationCodeResult.Failure.InvalidEmail -> CreateAccountCodeViewState.CodeError.DialogError.InvalidEmailError
        is RequestActivationCodeResult.Failure.Generic -> CreateAccountCodeViewState.CodeError.DialogError.GenericError(this.failure)
        RequestActivationCodeResult.Success -> CreateAccountCodeViewState.CodeError.None
    }
}
