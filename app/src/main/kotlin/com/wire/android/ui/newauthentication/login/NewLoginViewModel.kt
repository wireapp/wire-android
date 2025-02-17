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
import com.wire.android.config.DefaultServerConfig
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel.Companion.USER_IDENTIFIER_SAVED_STATE_KEY
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.EnterpriseLoginResult
import com.wire.kalium.logic.feature.auth.LoginRedirectPath
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
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
) : ViewModel() {
    private val loginNavArgs: LoginNavArgs = savedStateHandle.navArgs()
    private val preFilledUserIdentifier: PreFilledUserIdentifierType = loginNavArgs.userHandle.let {
        if (it.isNullOrEmpty()) PreFilledUserIdentifierType.None else PreFilledUserIdentifierType.PreFilled(it)
    }

    var state by mutableStateOf(NewLoginScreenState())
        private set
    val userIdentifierTextState: TextFieldState = TextFieldState()
    var loginEmailSSOState by mutableStateOf(NewLoginEmailSSOState())

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
    fun onLoginStarted(onSuccess: (LoginRedirectPath) -> Unit) {
        viewModelScope.launch {
            updateLoginFlowState(DomainCheckupState.Loading)
            val sanitizedInput = userIdentifierTextState.text.trim().toString()
            when (validateEmailOrSSOCode(sanitizedInput)) {
                ValidateEmailOrSSOCodeUseCase.Result.InvalidInput -> {
                    updateLoginFlowState(DomainCheckupState.Error.TextFieldError.InvalidValue)
                    return@launch
                }

                ValidateEmailOrSSOCodeUseCase.Result.ValidEmail -> {
                    getEnterpriseLoginFlow(sanitizedInput, onSuccess)
                }

                ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode -> {
                    onSuccess(LoginRedirectPath.SSO(sanitizedInput))
                    updateLoginFlowState(DomainCheckupState.Default)
                }
            }
        }
    }

    private suspend fun getEnterpriseLoginFlow(email: String, onSuccess: (LoginRedirectPath) -> Unit) {
        val authScope = getAuthenticationScope()
        when (val loginFlowResult = authScope.getLoginFlowForDomainUseCase(email)) {
            is EnterpriseLoginResult.Failure.Generic -> updateLoginFlowState(
                DomainCheckupState.Error.DialogError.GenericError(
                    loginFlowResult.coreFailure
                )
            )

            EnterpriseLoginResult.Failure.NotSupported -> updateLoginFlowState(DomainCheckupState.Error.DialogError.NotSupported)
            is EnterpriseLoginResult.Success -> {
                onSuccess(loginFlowResult.loginRedirectPath)
                updateLoginFlowState(DomainCheckupState.Default)
            }
        }
    }

    private suspend fun getAuthenticationScope(): AuthenticationScope {
        return coreLogic.versionedAuthenticationScope(DefaultServerConfig).invoke(null).let {
            when (it) {
                is AutoVersionAuthScopeUseCase.Result.Failure.Generic,
                AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
                AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                    TODO("error handling in case of failure")
                }

                is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope
            }
        }
    }

    fun onDismissDialog() {
        updateLoginFlowState(DomainCheckupState.Default)
    }

    /**
     * Update the state based on the input.
     */
    private fun updateLoginFlowState(flowState: DomainCheckupState) {
        val currentUserLoginInput = userIdentifierTextState.text
        loginEmailSSOState = loginEmailSSOState.copy(
            flowState = flowState,
            nextEnabled = loginEmailSSOState.flowState !is DomainCheckupState.Loading
                    && currentUserLoginInput.isNotEmpty()
        )
    }
}
