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
package com.wire.android.ui.home.settings.account.email.updateEmail

import androidx.annotation.VisibleForTesting
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    private val updateEmail: UpdateEmailUseCase,
    private val getSelf: GetSelfUserUseCase,
) : ViewModel() {

    var state: ChangeEmailState by mutableStateOf(
        ChangeEmailState(
            isEmailTextEditEnabled = true
        )
    )
        @VisibleForTesting
        set

    private var currentEmail: String? = null

    init {
        viewModelScope.launch {
            getSelf().firstOrNull()?.email?.let {
                currentEmail = it
                state = state.copy(email = TextFieldValue(it))
            } ?: run {
                state = state.copy(flowState = ChangeEmailState.FlowState.Error.SelfUserNotFound)
            }
        }
    }

    fun onSaveClicked() {
        state = state.copy(saveEnabled = false, isEmailTextEditEnabled = false, flowState = ChangeEmailState.FlowState.Loading)
        viewModelScope.launch {
            when (updateEmail(state.email.text)) {
                UpdateEmailUseCase.Result.Failure.EmailAlreadyInUse -> state =
                    state.copy(
                        isEmailTextEditEnabled = true,
                        saveEnabled = false,
                        flowState = ChangeEmailState.FlowState.Error.TextFieldError.AlreadyInUse,
                    )

                UpdateEmailUseCase.Result.Failure.InvalidEmail -> state =
                    state.copy(
                        isEmailTextEditEnabled = true,
                        saveEnabled = false,
                        flowState = ChangeEmailState.FlowState.Error.TextFieldError.InvalidEmail
                    )

                is UpdateEmailUseCase.Result.Failure.GenericFailure -> state =
                    state.copy(
                        isEmailTextEditEnabled = true,
                        saveEnabled = false,
                        flowState = ChangeEmailState.FlowState.Error.TextFieldError.Generic
                    )

                is UpdateEmailUseCase.Result.Success.VerificationEmailSent ->
                    state = state.copy(flowState = ChangeEmailState.FlowState.Success(state.email.text))
                is UpdateEmailUseCase.Result.Success.NoChange ->
                    state = state.copy(flowState = ChangeEmailState.FlowState.NoChange)
            }
        }
    }

    fun onEmailChange(newEmail: TextFieldValue) {
        val cleanEmail = newEmail.text.trim()
        val isValidEmail = Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()
        when {
            cleanEmail.isBlank() -> state =
                state.copy(
                    saveEnabled = false,
                    email = newEmail,
                )

            cleanEmail == currentEmail -> state = state.copy(
                saveEnabled = false,
                email = newEmail,
                flowState = ChangeEmailState.FlowState.Default
            )

            else -> state = state.copy(
                saveEnabled = isValidEmail,
                email = newEmail,
                flowState = ChangeEmailState.FlowState.Default
            )
        }
    }

    fun onEmailErrorAnimated() {
        state = state.copy(animatedEmailError = false)
    }
}
