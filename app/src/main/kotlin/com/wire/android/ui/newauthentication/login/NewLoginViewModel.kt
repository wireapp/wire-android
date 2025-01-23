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
import com.wire.android.di.AuthServerConfigProvider
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

@HiltViewModel
class NewLoginViewModel @Inject constructor(
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val loginNavArgs: LoginNavArgs = savedStateHandle.navArgs()
    private val preFilledUserIdentifier: PreFilledUserIdentifierType = loginNavArgs.userHandle.let {
        if (it.isNullOrEmpty()) PreFilledUserIdentifierType.None else PreFilledUserIdentifierType.PreFilled(it)
    }

    var state by mutableStateOf(
        NewLoginScreenState(
            links = ServerConfig.DEFAULT,
        )
    )
        private set
    val userIdentifierTextState: TextFieldState = TextFieldState()
    var loginState by mutableStateOf(LoginEmailState())

    init {
        observerAuthServer()
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
                updateEmailFlowState(it)
            }
        }
    }

    /**
     * Starts the login flow, this will check against BE if email or sso code and relay to the corresponding flow afterwards.
     */
    fun onLoginStarted(onSuccess: () -> Unit) {
        viewModelScope.launch {
            loginState = loginState.copy(flowState = LoginState.Loading)
            @Suppress("MagicNumber") delay(1000) // TODO(ym): here the call to the use case should be done.
            loginState = loginState.copy(flowState = LoginState.Default)
            onSuccess()
        }
    }

    /**
     * Update the state based on the input.
     * TODO(ym): Check if we need to validate the email, since this an SSO code can also be valid in this input.
     */
    private fun updateEmailFlowState(email: CharSequence) {
        loginState = loginState.copy(
            flowState = LoginState.Default,
            loginEnabled = email.isNotEmpty() && loginState.flowState !is LoginState.Loading
        )
    }

    private fun observerAuthServer() {
        viewModelScope.launch {
            authServerConfigProvider.authServer.collect {
                state = state.copy(links = it)
            }
        }
    }
}
