/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.newauthentication.login

import androidx.annotation.VisibleForTesting
import com.wire.android.appLogger
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.config.orDefault
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel.Companion.USER_IDENTIFIER_SAVED_STATE_KEY
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelExtension
import com.wire.android.ui.authentication.login.sso.SSOUrlConfig
import com.wire.android.ui.authentication.login.sso.ssoCodeWithPrefix
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.EnterpriseLoginResult
import com.wire.kalium.logic.feature.auth.LoginRedirectPath
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.FetchSSOSettingsUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class NewLoginViewModel(
    private val validateEmailOrSSOCode: ValidateEmailOrSSOCodeUseCase,
    val coreLogic: CoreLogic,
    savedStateHandle: SavedStateHandle,
    val clientScopeProviderFactory: ClientScopeProvider.Factory,
    val userDataStoreProvider: UserDataStoreProvider,
    private val loginExtension: LoginViewModelExtension,
    private val ssoExtension: LoginSSOViewModelExtension,
    private val dispatchers: DispatcherProvider,
) : ActionsViewModel<NewLoginAction>() {

    @Inject
    constructor(
        validateEmailOrSSOCode: ValidateEmailOrSSOCodeUseCase,
        @KaliumCoreLogic coreLogic: CoreLogic,
        savedStateHandle: SavedStateHandle,
        addAuthenticatedUser: AddAuthenticatedUserUseCase,
        clientScopeProviderFactory: ClientScopeProvider.Factory,
        userDataStoreProvider: UserDataStoreProvider,
        dispatchers: DispatcherProvider,
    ) : this(
        validateEmailOrSSOCode,
        coreLogic,
        savedStateHandle,
        clientScopeProviderFactory,
        userDataStoreProvider,
        LoginViewModelExtension(clientScopeProviderFactory, userDataStoreProvider),
        LoginSSOViewModelExtension(addAuthenticatedUser, coreLogic),
        dispatchers,
    )

    private val loginNavArgs: LoginNavArgs = savedStateHandle.navArgs()
    private val preFilledUserIdentifier: PreFilledUserIdentifierType = loginNavArgs.userHandle ?: PreFilledUserIdentifierType.None
    var serverConfig: ServerConfig.Links by mutableStateOf(loginNavArgs.loginPasswordPath?.customServerConfig.orDefault())
        private set

    var state by mutableStateOf(NewLoginScreenState())
        private set
    val userIdentifierTextState: TextFieldState = TextFieldState()

    init {
        userIdentifierTextState.setTextAndPlaceCursorAtEnd(
            if (preFilledUserIdentifier is PreFilledUserIdentifierType.PreFilled) {
                preFilledUserIdentifier.userIdentifier
            } else {
                savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] ?: String.EMPTY
            }
        )
        viewModelScope.launch {
            userIdentifierTextState.textAsFlow().distinctUntilChanged().onEach {
                savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] = it.toString()
            }.collectLatest {
                getAndUpdateLoginFlowState { currentState: NewLoginFlowState ->
                    if (currentState is NewLoginFlowState.Error.TextFieldError) NewLoginFlowState.Default else currentState
                }
            }
        }

        // Fetch default SSO code for the server configuration
        if (userIdentifierTextState.text.isEmpty() && preFilledUserIdentifier is PreFilledUserIdentifierType.None) {
            viewModelScope.launch(dispatchers.io()) {
                appLogger.d("NewLoginViewModel: Fetching default SSO code for server")
                ssoExtension.fetchDefaultSSOCode(
                    serverConfig = serverConfig,
                    onAuthScopeFailure = { error ->
                        appLogger.e("NewLoginViewModel: Failed to create auth scope for SSO settings: $error")
                    },
                    onFetchSSOSettingsFailure = { error ->
                        appLogger.e("NewLoginViewModel: Failed to fetch SSO settings: $error")
                    },
                    onSuccess = { defaultSSOCode ->
                        if (defaultSSOCode != null && userIdentifierTextState.text.isEmpty()) {
                            appLogger.d("NewLoginViewModel: Successfully fetched default SSO code $defaultSSOCode")
                            withContext(dispatchers.main()) {
                                userIdentifierTextState.setTextAndPlaceCursorAtEnd(defaultSSOCode)
                                savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] = defaultSSOCode
                            }
                        } else {
                            appLogger.d("NewLoginViewModel: No default SSO code configured for this server")
                        }
                    }
                )
            }
        }
    }

    /**
     * Starts the login flow, this will check against BE if email or sso code and relay to the corresponding flow afterwards.
     */
    fun onLoginStarted() {
        viewModelScope.launch(dispatchers.io()) {
            updateLoginFlowState(NewLoginFlowState.Loading)
            val sanitizedInput = userIdentifierTextState.text.trim().toString()
            when (validateEmailOrSSOCode(sanitizedInput)) {
                ValidateEmailOrSSOCodeUseCase.Result.InvalidInput -> {
                    updateLoginFlowState(NewLoginFlowState.Error.TextFieldError.InvalidValue)
                    return@launch
                }

                ValidateEmailOrSSOCodeUseCase.Result.ValidEmail -> {
                    getEnterpriseLoginFlow(sanitizedInput)
                }

                ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode -> {
                    initiateSSO(serverConfig, sanitizedInput)
                }
            }
        }
    }

    @VisibleForTesting
    internal suspend fun getEnterpriseLoginFlow(email: String) = withContext(dispatchers.io()) {
        ssoExtension.withAuthenticationScope(
            serverConfig = serverConfig,
            onAuthScopeFailure = { updateLoginFlowState(it.toLoginError()) },
            onSuccess = { authScope ->
                when (val loginFlowResult = authScope.getLoginFlowForDomainUseCase(email)) {
                    is EnterpriseLoginResult.Failure.Generic -> withContext(dispatchers.main()) {
                        updateLoginFlowState(NewLoginFlowState.Error.DialogError.GenericError(loginFlowResult.coreFailure))
                    }

                    is EnterpriseLoginResult.Failure.NotSupported -> withContext(dispatchers.main()) {
                        sendAction(NewLoginAction.EnterpriseLoginNotSupported(email))
                        updateLoginFlowState(NewLoginFlowState.Default)
                    }

                    is EnterpriseLoginResult.Success -> {
                        when (val loginRedirectPath = loginFlowResult.loginRedirectPath) {
                            is LoginRedirectPath.SSO -> {
                                initiateSSO(serverConfig, loginRedirectPath.ssoCode.ssoCodeWithPrefix())
                            }

                            is LoginRedirectPath.CustomBackend -> withContext(dispatchers.main()) {
                                updateLoginFlowState(NewLoginFlowState.CustomConfigDialog(loginRedirectPath.serverLinks))
                            }

                            is LoginRedirectPath.Default,
                            is LoginRedirectPath.NoRegistration -> withContext(dispatchers.main()) {
                                sendAction(
                                    NewLoginAction.EmailPassword(
                                        userIdentifier = email,
                                        loginPasswordPath = LoginPasswordPath(
                                            customServerConfig = loginNavArgs.loginPasswordPath?.customServerConfig,
                                            isCloudAccountCreationPossible = loginRedirectPath.isCloudAccountCreationPossible,
                                        )
                                    )
                                )
                                updateLoginFlowState(NewLoginFlowState.Default)
                            }

                            is LoginRedirectPath.ExistingAccountWithClaimedDomain -> withContext(dispatchers.main()) {
                                sendAction(
                                    NewLoginAction.EmailPassword(
                                        userIdentifier = email,
                                        loginPasswordPath = LoginPasswordPath(
                                            customServerConfig = loginNavArgs.loginPasswordPath?.customServerConfig,
                                            isCloudAccountCreationPossible = loginRedirectPath.isCloudAccountCreationPossible,
                                            isDomainClaimedByOrg = DomainClaimedByOrg.Claimed(
                                                loginRedirectPath.domain
                                            ),
                                        )
                                    )
                                )
                                updateLoginFlowState(NewLoginFlowState.Default)
                            }
                        }
                    }
                }
            }
        )
    }

    fun onDismissDialog() {
        updateLoginFlowState(NewLoginFlowState.Default)
    }

    fun onCustomServerDialogConfirm(customServerConfig: ServerConfig.Links) {
        viewModelScope.launch(dispatchers.io()) {
            ssoExtension.fetchDefaultSSOCode(
                serverConfig = customServerConfig,
                onAuthScopeFailure = { updateLoginFlowState(it.toLoginError()) },
                onFetchSSOSettingsFailure = { updateLoginFlowState(it.toLoginError()) },
                onSuccess = { defaultSSOCode ->
                    appLogger.d("NewLoginViewModel: Successfully fetched default SSO code $defaultSSOCode")

                    when {
                        defaultSSOCode != null -> {
                            initiateSSO(customServerConfig, defaultSSOCode)
                        }

                        else -> withContext(dispatchers.main()) {
                            sendAction(NewLoginAction.CustomConfig(userIdentifierTextState.text.toString(), customServerConfig))
                            updateLoginFlowState(NewLoginFlowState.Default)
                        }
                    }
                }
            )
        }
    }

    @VisibleForTesting
    internal suspend fun initiateSSO(serverConfig: ServerConfig.Links, ssoCode: String) =
        withContext(dispatchers.io()) {
            ssoExtension.initiateSSO(
                serverConfig = serverConfig,
                ssoCode = ssoCode,
                onAuthScopeFailure = { updateLoginFlowState(it.toLoginError()) },
                onSSOInitiateFailure = { updateLoginFlowState(it.toLoginError()) },
                onSuccess = { requestUrl, serverConfig ->
                    withContext(dispatchers.main()) {
                        updateLoginFlowState(NewLoginFlowState.Default)
                        sendAction(NewLoginAction.SSO(requestUrl, SSOUrlConfig(serverConfig, userIdentifierTextState.text.toString())))
                        updateLoginFlowState(NewLoginFlowState.Default)
                    }
                }
            )
        }

    fun handleSSOResult(ssoLoginResult: DeepLinkResult.SSOLogin, config: SSOUrlConfig?) {
        updateLoginFlowState(NewLoginFlowState.Loading)
        if (config != null) {
            serverConfig = config.serverConfig
            userIdentifierTextState.setTextAndPlaceCursorAtEnd(config.userIdentifier)
        }
        when (ssoLoginResult) {
            is DeepLinkResult.SSOLogin.Success -> {
                viewModelScope.launch(dispatchers.io()) {
                    ssoExtension.establishSSOSession(
                        cookie = ssoLoginResult.cookie,
                        serverConfigId = ssoLoginResult.serverConfigId,
                        serverConfig = config?.serverConfig ?: serverConfig,
                        onAuthScopeFailure = { updateLoginFlowState(it.toLoginError()) },
                        onSSOLoginFailure = { updateLoginFlowState(it.toLoginError()) },
                        onAddAuthenticatedUserFailure = { updateLoginFlowState(it.toLoginError()) },
                        onSuccess = { storedUserId ->
                            loginExtension.registerClient(storedUserId, null).let { result ->
                                withContext(dispatchers.main()) {
                                    when (result) {
                                        is RegisterClientResult.Success -> {
                                            when (loginExtension.isInitialSyncCompleted(storedUserId)) {
                                                true -> sendAction(NewLoginAction.Success(NewLoginAction.Success.NextStep.None))
                                                false -> sendAction(NewLoginAction.Success(NewLoginAction.Success.NextStep.InitialSync))
                                            }
                                            updateLoginFlowState(NewLoginFlowState.Default)
                                        }

                                        is RegisterClientResult.E2EICertificateRequired -> {
                                            sendAction(NewLoginAction.Success(NewLoginAction.Success.NextStep.E2EIEnrollment))
                                            updateLoginFlowState(NewLoginFlowState.Default)
                                        }

                                        is RegisterClientResult.Failure.TooManyClients -> {
                                            sendAction(NewLoginAction.Success(NewLoginAction.Success.NextStep.TooManyDevices))
                                            updateLoginFlowState(NewLoginFlowState.Default)
                                        }

                                        is RegisterClientResult.Failure.Generic ->
                                            updateLoginFlowState(NewLoginFlowState.Error.DialogError.GenericError(result.genericFailure))

                                        is RegisterClientResult.Failure.InvalidCredentials,
                                        is RegisterClientResult.Failure.PasswordAuthRequired -> { // for SSO login these should not happen
                                            val failure = CoreFailure.Unknown(IllegalStateException(result::class.simpleName ?: "Unknown"))
                                            updateLoginFlowState(NewLoginFlowState.Error.DialogError.GenericError(failure))
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            is DeepLinkResult.SSOLogin.Failure -> {
                updateLoginFlowState(NewLoginFlowState.Error.DialogError.SSOResultFailure(ssoLoginResult.ssoError))
            }
        }
    }

    /**
     * Update the state based on the input.
     */
    private fun updateLoginFlowState(flowState: NewLoginFlowState) = getAndUpdateLoginFlowState { flowState }

    /**
     * Update the state based on the current state and input.
     */
    private fun getAndUpdateLoginFlowState(update: (NewLoginFlowState) -> NewLoginFlowState) = viewModelScope.launch(dispatchers.main()) {
            val newState = update(state.flowState)
            val currentUserLoginInput = userIdentifierTextState.text
            state = state.copy(
                flowState = newState,
                nextEnabled = newState !is NewLoginFlowState.Loading && currentUserLoginInput.isNotEmpty()
            )
        }
}

private fun AutoVersionAuthScopeUseCase.Result.Failure.toLoginError() = when (this) {
    is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> NewLoginFlowState.Error.DialogError.GenericError(genericFailure)
    is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> NewLoginFlowState.Error.DialogError.ClientUpdateRequired
    is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> NewLoginFlowState.Error.DialogError.ServerVersionNotSupported
}

private fun FetchSSOSettingsUseCase.Result.Failure.toLoginError() = NewLoginFlowState.Error.DialogError.GenericError(coreFailure)

private fun SSOInitiateLoginResult.Failure.toLoginError() = when (this) {
    is SSOInitiateLoginResult.Failure.InvalidCodeFormat -> NewLoginFlowState.Error.TextFieldError.InvalidValue
    is SSOInitiateLoginResult.Failure.InvalidCode -> NewLoginFlowState.Error.DialogError.InvalidSSOCode
    is SSOInitiateLoginResult.Failure.Generic -> NewLoginFlowState.Error.DialogError.GenericError(this.genericFailure)
    is SSOInitiateLoginResult.Failure.InvalidRedirect ->
        NewLoginFlowState.Error.DialogError.GenericError(CoreFailure.Unknown(IllegalArgumentException("Invalid Redirect")))
}

private fun SSOLoginSessionResult.Failure.toLoginError() = when (this) {
    is SSOLoginSessionResult.Failure.InvalidCookie -> NewLoginFlowState.Error.DialogError.InvalidSSOCookie
    is SSOLoginSessionResult.Failure.Generic -> NewLoginFlowState.Error.DialogError.GenericError(this.genericFailure)
}

private fun AddAuthenticatedUserUseCase.Result.Failure.toLoginError() = when (this) {
    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> NewLoginFlowState.Error.DialogError.GenericError(this.genericFailure)
    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists -> NewLoginFlowState.Error.DialogError.UserAlreadyExists
}
