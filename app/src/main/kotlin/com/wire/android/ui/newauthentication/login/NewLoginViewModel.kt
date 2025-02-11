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
import com.wire.android.config.orDefault
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.email.LoginEmailState
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel.Companion.USER_IDENTIFIER_SAVED_STATE_KEY
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.configuration.server.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class NewLoginViewModel @Inject constructor(
    private val validateEmailOrSSOCode: ValidateEmailOrSSOCodeUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val loginNavArgs: LoginNavArgs = savedStateHandle.navArgs()
    private val preFilledUserIdentifier: PreFilledUserIdentifierType = loginNavArgs.userHandle.let {
        if (it.isNullOrEmpty()) PreFilledUserIdentifierType.None else PreFilledUserIdentifierType.PreFilled(it)
    }

    var state by mutableStateOf(NewLoginScreenState())
        private set
    val userIdentifierTextState: TextFieldState = TextFieldState()
    var loginState by mutableStateOf(LoginEmailState())

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
                updateLoginFlowState(LoginState.Default)
            }
        }
    }

    /**
     * Starts the login flow, this will check against BE if email or sso code and relay to the corresponding flow afterwards.
     */
    fun onLoginStarted(onSuccess: (ServerConfig.Links) -> Unit) {
        viewModelScope.launch {
            if (validateEmailOrSSOCode(userIdentifierTextState.text.trim())) {
                updateLoginFlowState(LoginState.Loading)
                delay(1.seconds) // TODO(ym): here the call to the use case should be done.
                updateLoginFlowState(LoginState.Default)
                onSuccess(DefaultServerConfig) // TODO: pass custom server config if use case returns it
            } else {
                updateLoginFlowState(LoginState.Error.TextFieldError.InvalidValue)
                return@launch
            }
        }
    }

    /**
     * Update the state based on the input.
     */
    private fun updateLoginFlowState(flowState: LoginState) {
        val currentUserLoginInput = userIdentifierTextState.text
        loginState = loginState.copy(
            flowState = flowState,
            loginEnabled = loginState.flowState !is LoginState.Loading
                    && currentUserLoginInput.isNotEmpty()
        )
    }
}
