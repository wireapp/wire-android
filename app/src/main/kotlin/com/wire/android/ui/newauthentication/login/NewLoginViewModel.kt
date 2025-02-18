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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.config.orDefault
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel.Companion.USER_IDENTIFIER_SAVED_STATE_KEY
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelExtension
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialogState
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.EnterpriseLoginResult
import com.wire.kalium.logic.feature.auth.LoginRedirectPath
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewLoginViewModel @Inject constructor(
    private val validateEmailOrSSOCode: ValidateEmailOrSSOCodeUseCase,
    @KaliumCoreLogic val coreLogic: CoreLogic,
    private val savedStateHandle: SavedStateHandle,
    addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    userDataStoreProvider: UserDataStoreProvider,
) : ViewModel() {
    private val loginExtension = LoginViewModelExtension(clientScopeProviderFactory, userDataStoreProvider)
    private val ssoExtension = LoginSSOViewModelExtension(addAuthenticatedUser, coreLogic)
    private val loginNavArgs: LoginNavArgs = savedStateHandle.navArgs()
    private val preFilledUserIdentifier: PreFilledUserIdentifierType = loginNavArgs.userHandle.let {
        if (it.isNullOrEmpty()) PreFilledUserIdentifierType.None else PreFilledUserIdentifierType.PreFilled(it)
    }
    val serverConfig: ServerConfig.Links = loginNavArgs.loginPasswordPath?.customServerConfig.orDefault()

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
                updateLoginFlowState(DomainCheckupState.Default)
            }
        }
    }

    /**
     * Starts the login flow, this will check against BE if email or sso code and relay to the corresponding flow afterwards.
     */
    fun onLoginStarted(action: (NewLoginAction) -> Unit) {
        viewModelScope.launch {
            updateLoginFlowState(DomainCheckupState.Loading)
            val sanitizedInput = userIdentifierTextState.text.trim().toString()
            when (validateEmailOrSSOCode(sanitizedInput)) {
                ValidateEmailOrSSOCodeUseCase.Result.InvalidInput -> {
                    updateLoginFlowState(DomainCheckupState.Error.TextFieldError.InvalidValue)
                    return@launch
                }

                ValidateEmailOrSSOCodeUseCase.Result.ValidEmail -> {
                    getEnterpriseLoginFlow(sanitizedInput, action)
                }

                ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode -> {
                    initiateSSO(serverConfig, sanitizedInput, action)
                }
            }
        }
    }

    private suspend fun getEnterpriseLoginFlow(email: String, action: (NewLoginAction) -> Unit) {
        ssoExtension.withAuthenticationScope(
            serverConfig = serverConfig,
            onAuthScopeFailure = { authScopeFailure -> TODO("handle auth scope error") },
            onSuccess = { authScope ->
                when (val loginFlowResult = authScope.getLoginFlowForDomainUseCase(email)) {
                    is EnterpriseLoginResult.Failure.Generic -> updateLoginFlowState(
                        DomainCheckupState.Error.DialogError.GenericError(
                            loginFlowResult.coreFailure
                        )
                    )

                    is EnterpriseLoginResult.Failure.NotSupported -> updateLoginFlowState(DomainCheckupState.Error.DialogError.NotSupported)

                    is EnterpriseLoginResult.Success -> {
                        when (val loginRedirectPath = loginFlowResult.loginRedirectPath) {
                            is LoginRedirectPath.SSO -> {
                                initiateSSO(serverConfig, loginRedirectPath.ssoCode, action)
                            }

                            is LoginRedirectPath.CustomBackend -> {
                                state = state.copy(
                                    customServerDialogState = CustomServerDetailsDialogState(loginRedirectPath.serverLinks),
                                )
                                updateLoginFlowState(DomainCheckupState.Default)
                            }

                            is LoginRedirectPath.Default,
                            is LoginRedirectPath.NoRegistration -> {
                                action(
                                    NewLoginAction.EmailPassword(
                                        userIdentifier = userIdentifierTextState.text.toString(),
                                        loginPasswordPath = LoginPasswordPath(
                                            isCloudAccountCreationPossible = loginRedirectPath.isCloudAccountCreationPossible,
                                        )
                                    )
                                )
                                updateLoginFlowState(DomainCheckupState.Default)
                            }

                            is LoginRedirectPath.ExistingAccountWithClaimedDomain -> {
                                action(
                                    NewLoginAction.EmailPassword(
                                        userIdentifier = userIdentifierTextState.text.toString(),
                                        loginPasswordPath = LoginPasswordPath(
                                            isCloudAccountCreationPossible = loginRedirectPath.isCloudAccountCreationPossible,
                                            isDomainClaimedByOrg = true,
                                        )
                                    )
                                )
                                updateLoginFlowState(DomainCheckupState.Default)
                            }
                        }
                    }
                }
            }
        )
    }

    fun onDismissDialog() {
        updateLoginFlowState(DomainCheckupState.Default)
    }

    fun onCustomServerDialogDismiss() {
        state = state.copy(customServerDialogState = null)
    }

    fun onCustomServerDialogConfirm(customServerConfig: ServerConfig.Links, action: (NewLoginAction) -> Unit) {
        viewModelScope.launch {
            ssoExtension.fetchDefaultSSOCode(
                serverConfig = customServerConfig,
                onAuthScopeFailure = { authScopeFailure -> TODO("handle auth scope error") },
                onFetchSSOSettingsFailure = { fetchSSOSettingsFailure -> TODO("handle fetch sso settings error") },
                onSuccess = { defaultSSOCode ->
                    when {
                        defaultSSOCode != null -> {
                            initiateSSO(serverConfig, defaultSSOCode, action)
                        }
                        else -> {
                            action(NewLoginAction.CustomConfig(userIdentifierTextState.text.toString(), customServerConfig))
                            updateLoginFlowState(DomainCheckupState.Default)
                        }
                    }
                }
            )
        }
    }

    private suspend fun initiateSSO(serverConfig: ServerConfig.Links, ssoCode: String, action: (NewLoginAction) -> Unit) {
        ssoExtension.initiateSSO(
            serverConfig = serverConfig,
            ssoCode = ssoCode,
            onAuthScopeFailure = { authScopeFailure -> TODO("handle auth scope error") },
            onSSOInitiateFailure = { ssoInitiateFailure -> TODO("handle sso initiate error") },
            onSuccess = { requestUrl, _ ->
                updateLoginFlowState(DomainCheckupState.Default)
                action(NewLoginAction.SSO(requestUrl))
            }
        )
    }

    fun handleSSOResult(ssoLoginResult: DeepLinkResult.SSOLogin, serverConfig: ServerConfig.Links?, action: (NewLoginAction) -> Unit) {
        when (ssoLoginResult) {
            is DeepLinkResult.SSOLogin.Success -> {
                updateLoginFlowState(DomainCheckupState.Loading)
                viewModelScope.launch {
                    ssoExtension.establishSSOSession(
                        cookie = ssoLoginResult.cookie,
                        serverConfigId = ssoLoginResult.serverConfigId,
                        serverConfig = serverConfig ?: this@NewLoginViewModel.serverConfig,
                        onAuthScopeFailure = { authScopeFailure -> TODO("handle auth scope error") },
                        onSSOLoginFailure = { ssoLoginFailure -> TODO("handle sso login error") },
                        onAddAuthenticatedUserFailure = { addAuthenticatedUserFailure -> TODO("handle add user error") },
                        onSuccess = { storedUserId ->
                            when (loginExtension.registerClient(storedUserId, null)) {
                                is RegisterClientResult.Success -> when (loginExtension.isInitialSyncCompleted(storedUserId)) {
                                    true -> action(NewLoginAction.Success(NewLoginAction.Success.NextStep.None))
                                    false -> action(NewLoginAction.Success(NewLoginAction.Success.NextStep.InitialSync))
                                }
                                is RegisterClientResult.E2EICertificateRequired ->
                                    action(NewLoginAction.Success(NewLoginAction.Success.NextStep.E2EIEnrollment))
                                is RegisterClientResult.Failure.TooManyClients ->
                                    action(NewLoginAction.Success(NewLoginAction.Success.NextStep.TooManyDevices))
                                is RegisterClientResult.Failure -> {
                                    TODO("handle register client error")
                                }
                            }
                        }
                    )
                }
            }
            is DeepLinkResult.SSOLogin.Failure -> {
                TODO("handle SSO deeplink error")
            }
        }
    }

    /**
     * Update the state based on the input.
     */
    private fun updateLoginFlowState(flowState: DomainCheckupState) {
        val currentUserLoginInput = userIdentifierTextState.text
        state = state.copy(
            flowState = flowState,
            nextEnabled = flowState !is DomainCheckupState.Loading
                    && currentUserLoginInput.isNotEmpty()
        )
    }
}
