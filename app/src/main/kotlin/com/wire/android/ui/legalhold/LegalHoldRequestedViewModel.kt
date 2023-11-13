/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.legalhold

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalHoldRequestedViewModel @Inject constructor(
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    ) : ViewModel() {

    var state: LegalHoldRequestedState by mutableStateOf(LegalHoldRequestedState())
        private set

    init {
        state = state.copy(legalHoldDeviceFingerprint = "0123456789ABCDEF")// TODO get fingerprint
        viewModelScope.launch {
            isPasswordRequired().let {
                state = state.copy(requiresPassword = (it as? IsPasswordRequiredUseCase.Result.Success)?.value ?: true)
            }
        }
    }

    fun passwordChanged(password: TextFieldValue) {
        state = state.copy(password = password)
        validatePassword(password.text).let {
            state = state.copy(acceptEnabled = it.isValid)
        }
    }

    fun notNowClicked() {
        // TODO
    }

    fun acceptClicked() {
        state = state.copy(acceptEnabled = false, loading = true)
        // the accept button is enabled if the password is valid, this check is for safety only
        validatePassword(state.password.text).let {
            if (!it.isValid) {
                state = state.copy(loading = false, error = LegalHoldRequestedError.InvalidCredentialsError)
            }
            if (it.isValid) {
                viewModelScope.launch {
                    // TODO
                    state = state.copy(loading = false, done = true)
                }
            }
        }
    }
}
