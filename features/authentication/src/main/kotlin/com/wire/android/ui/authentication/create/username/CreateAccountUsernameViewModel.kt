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

package com.wire.android.ui.authentication.create.username

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch

class CreateAccountUsernameViewModel<FailureT>(
    private val gateway: CreateAccountUsernameGateway<FailureT>,
    private val analytics: CreateAccountUsernameAnalytics,
) : ViewModel() {

    val textState: TextFieldState = TextFieldState()
    var state: CreateAccountUsernameViewState<FailureT> by mutableStateOf(CreateAccountUsernameViewState())
        private set

    init {
        viewModelScope.launch {
            analytics.usernameScreenShown()
            textState.textAsFlow()
                .dropWhile { it.isEmpty() } // ignore first empty value to not show the error before the user typed anything
                .collectLatest { newHandle ->
                    gateway.validateUsername(newHandle.toString()).let { validateResult ->
                        state = when (validateResult) {
                            UsernameValidation.Valid -> state.copy(
                                error = CreateAccountUsernameError.None,
                                continueEnabled = !state.loading,
                            )

                            UsernameValidation.Invalid -> state.copy(
                                error = CreateAccountUsernameError.UsernameInvalid,
                                continueEnabled = false,
                            )
                        }
                    }
                }
        }
    }

    fun onErrorDismiss() {
        state = state.copy(error = CreateAccountUsernameError.None)
    }

    fun onContinue() {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            val usernameError = when (val result = gateway.setUsername(textState.text.toString().trim())) {
                is SetUsernameResult.Failure -> CreateAccountUsernameError.Generic(result.failure)
                SetUsernameResult.UsernameTaken -> CreateAccountUsernameError.UsernameTaken
                SetUsernameResult.UsernameInvalid -> CreateAccountUsernameError.UsernameInvalid
                SetUsernameResult.Success -> {
                    analytics.accountCreationCompleted()
                    CreateAccountUsernameError.None
                }
            }
            state = state.copy(
                loading = false,
                continueEnabled = true,
                error = usernameError,
                success = usernameError is CreateAccountUsernameError.None,
            )
        }
    }
}
