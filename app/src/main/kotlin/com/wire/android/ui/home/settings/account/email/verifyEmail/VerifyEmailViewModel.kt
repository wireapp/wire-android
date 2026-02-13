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
package com.wire.android.ui.home.settings.account.email.verifyEmail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val updateEmail: UpdateEmailUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var state: VerifyEmailState by mutableStateOf(VerifyEmailState())
        private set

    private val verifyEmailNavArgs: VerifyEmailNavArgs = savedStateHandle.navArgs()
    val newEmail: String = verifyEmailNavArgs.newEmail

    fun onResendVerificationEmailClicked() {
        newEmail.let {
            state = state.copy(isResendEmailEnabled = false)
            viewModelScope.launch {
                when (updateEmail(newEmail)) {
                    UpdateEmailUseCase.Result.Failure.EmailAlreadyInUse,
                    is UpdateEmailUseCase.Result.Failure.GenericFailure,
                    UpdateEmailUseCase.Result.Failure.InvalidEmail,
                    UpdateEmailUseCase.Result.Success.VerificationEmailSent -> {
                        /*no-op*/
                    }

                    UpdateEmailUseCase.Result.Success.NoChange -> state = state.copy(noChange = true)
                }
                state = state.copy(isResendEmailEnabled = true)
            }
        }
    }
}
