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
package com.wire.android.ui.home.settings.account.handle

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.authentication.create.common.handle.HandleUpdateErrorState
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeHandleViewModel @Inject constructor(
    private val updateHandle: SetUserHandleUseCase,
    private val validateHandle: ValidateUserHandleUseCase,
    private val getSelf: GetSelfUserUseCase
) : ViewModel() {

    var state: ChangeHandleState by mutableStateOf(ChangeHandleState())
        @VisibleForTesting
        set

    private var currentHandle: String? = null

    init {
        viewModelScope.launch {
            getSelf().firstOrNull()?.handle.let {
                currentHandle = it
                state = state.copy(handle = TextFieldValue(it.orEmpty()))
            }
        }
    }

    fun onHandleChanged(newHandle: TextFieldValue) {
        state = state.copy(handle = newHandle)
        viewModelScope.launch {
            state = when (validateHandle(newHandle.text)) {
                is ValidateUserHandleResult.Invalid.InvalidCharacters,
                is ValidateUserHandleResult.Invalid.TooLong,
                is ValidateUserHandleResult.Invalid.TooShort -> state.copy(
                    error = HandleUpdateErrorState.TextFieldError.UsernameInvalidError,
                    animatedHandleError = true,
                    isSaveButtonEnabled = false
                )

                is ValidateUserHandleResult.Valid -> state.copy(
                    error = HandleUpdateErrorState.None,
                    isSaveButtonEnabled = newHandle.text != currentHandle
                )
            }
        }
    }

    fun onSaveClicked(onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = updateHandle(state.handle.text)) {
                is SetUserHandleResult.Failure.Generic -> state =
                    state.copy(error = HandleUpdateErrorState.DialogError.GenericError(result.error))

                SetUserHandleResult.Failure.HandleExists -> state =
                    state.copy(error = HandleUpdateErrorState.TextFieldError.UsernameTakenError)

                SetUserHandleResult.Failure.InvalidHandle -> state =
                    state.copy(error = HandleUpdateErrorState.TextFieldError.UsernameInvalidError)

                SetUserHandleResult.Success -> onSuccess()
            }
        }
    }

    fun onHandleErrorAnimated() {
        state = state.copy(animatedHandleError = false)
    }

    fun onErrorDismiss() {
        state = state.copy(error = HandleUpdateErrorState.None)
    }
}
