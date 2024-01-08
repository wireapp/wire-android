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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.authentication.create.common.handle.HandleUpdateErrorState
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountUsernameViewModel @Inject constructor(
    private val validateUserHandleUseCase: ValidateUserHandleUseCase,
    private val setUserHandleUseCase: SetUserHandleUseCase
) : ViewModel() {
    var state: CreateAccountUsernameViewState by mutableStateOf(CreateAccountUsernameViewState())
        private set

    fun onUsernameChange(newText: TextFieldValue) {
        state = validateUserHandleUseCase(newText.text).let { textState ->
            when (textState) {
                is ValidateUserHandleResult.Valid -> state.copy(
                    username = newText.copy(text = textState.handle),
                    error = HandleUpdateErrorState.None,
                    continueEnabled = !state.loading,
                    animateUsernameError = false
                )

                is ValidateUserHandleResult.Invalid.InvalidCharacters -> state.copy(
                    username = newText.copy(text = textState.handle),
                    error = HandleUpdateErrorState.None,
                    continueEnabled = !state.loading,
                    animateUsernameError = true
                )

                is ValidateUserHandleResult.Invalid.TooLong -> state.copy(
                    username = newText.copy(text = textState.handle),
                    error = HandleUpdateErrorState.None,
                    continueEnabled = false,
                    animateUsernameError = false
                )

                is ValidateUserHandleResult.Invalid.TooShort -> state.copy(
                    username = newText.copy(text = textState.handle),
                    error = HandleUpdateErrorState.None,
                    continueEnabled = false,
                    animateUsernameError = false
                )
            }
        }
    }

    fun onErrorDismiss() {
        state = state.copy(error = HandleUpdateErrorState.None)
    }

    fun onContinue(onSuccess: () -> Unit) {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            // FIXME: no need to check the handle again since it's checked every time the text change
            val usernameError = if (validateUserHandleUseCase(state.username.text.trim()) is ValidateUserHandleResult.Invalid) {
                HandleUpdateErrorState.TextFieldError.UsernameInvalidError
            } else {
                when (val result = setUserHandleUseCase(state.username.text.trim())) {
                    is SetUserHandleResult.Failure.Generic ->
                        HandleUpdateErrorState.DialogError.GenericError(result.error)

                    SetUserHandleResult.Failure.HandleExists ->
                        HandleUpdateErrorState.TextFieldError.UsernameTakenError

                    SetUserHandleResult.Failure.InvalidHandle ->
                        HandleUpdateErrorState.TextFieldError.UsernameInvalidError

                    SetUserHandleResult.Success -> HandleUpdateErrorState.None
                }
            }
            state = state.copy(loading = false, continueEnabled = true, error = usernameError)
            if (usernameError is HandleUpdateErrorState.None) {
                onSuccess()
            }
        }
    }

    fun onUsernameErrorAnimated() {
        state = state.copy(animateUsernameError = false)
    }
}
