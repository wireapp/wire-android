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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.Patterns
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    private val updateEmail: UpdateEmailUseCase,
    private val getSelf: GetSelfUserUseCase,
) : ViewModel() {

    val textState: TextFieldState = TextFieldState()
    var state: ChangeEmailState by mutableStateOf(ChangeEmailState())
        @VisibleForTesting
        set

    init {
        viewModelScope.launch {
            getSelf()?.email?.let { currentEmail ->
                textState.setTextAndPlaceCursorAtEnd(currentEmail)
                textState.textAsFlow().collectLatest {
                    val isValidEmail = Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches()
                    state = state.copy(
                        saveEnabled = it.trim().isNotEmpty() && isValidEmail && it.trim() != currentEmail,
                        flowState = ChangeEmailState.FlowState.Default
                    )
                }
            } ?: run {
                state = state.copy(flowState = ChangeEmailState.FlowState.Error.SelfUserNotFound)
            }
        }
    }

    fun onSaveClicked() {
        state = state.copy(saveEnabled = false, flowState = ChangeEmailState.FlowState.Loading)
        viewModelScope.launch {
            val email = textState.text.trim().toString()
            when (updateEmail(email)) {
                UpdateEmailUseCase.Result.Failure.EmailAlreadyInUse ->
                    state =
                    state.copy(
                        saveEnabled = false,
                        flowState = ChangeEmailState.FlowState.Error.TextFieldError.AlreadyInUse,
                    )

                UpdateEmailUseCase.Result.Failure.InvalidEmail ->
                    state =
                    state.copy(
                        saveEnabled = false,
                        flowState = ChangeEmailState.FlowState.Error.TextFieldError.InvalidEmail
                    )

                is UpdateEmailUseCase.Result.Failure.GenericFailure ->
                    state =
                    state.copy(
                        saveEnabled = false,
                        flowState = ChangeEmailState.FlowState.Error.TextFieldError.Generic
                    )

                is UpdateEmailUseCase.Result.Success.VerificationEmailSent ->
                    state = state.copy(flowState = ChangeEmailState.FlowState.Success(email))
                is UpdateEmailUseCase.Result.Success.NoChange ->
                    state = state.copy(flowState = ChangeEmailState.FlowState.NoChange)
            }
        }
    }
}
