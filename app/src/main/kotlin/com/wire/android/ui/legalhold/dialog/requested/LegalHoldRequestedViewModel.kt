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
package com.wire.android.ui.legalhold.dialog.requested

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LegalHoldRequestedViewModel @Inject constructor(
    private val validatePassword: ValidatePasswordUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) : ViewModel() {

    var state: LegalHoldRequestedState by mutableStateOf(LegalHoldRequestedState.Hidden)
        private set
    // TODO: get legal hold status of current account

    private fun LegalHoldRequestedState.ifVisible(action: (LegalHoldRequestedState.Visible) -> Unit) {
        if (this is LegalHoldRequestedState.Visible) action(this)
    }

    fun passwordChanged(password: TextFieldValue) {
        state.ifVisible {
            state = it.copy(password = password, acceptEnabled = validatePassword(password.text).isValid)
        }
    }

    fun notNowClicked() {
        state = LegalHoldRequestedState.Hidden
    }

    private suspend fun checkIfPasswordRequired(action: (Boolean) -> Unit) {
        when (val currentSessionResult = coreLogic.getGlobalScope().session.currentSession()) {
            CurrentSessionResult.Failure.SessionNotFound -> appLogger.e("$TAG: Session not found")
            is CurrentSessionResult.Failure.Generic -> appLogger.e("$TAG: Failed to get current session")
            is CurrentSessionResult.Success -> action(
                coreLogic.getSessionScope(currentSessionResult.accountInfo.userId).users.isPasswordRequired()
                    .let { (it as? IsPasswordRequiredUseCase.Result.Success)?.value ?: true }
            )
        }
    }

    fun show() {
        viewModelScope.launch {
            checkIfPasswordRequired { isPasswordRequired ->
                state = LegalHoldRequestedState.Visible(
                    requiresPassword = isPasswordRequired,
                    legalHoldDeviceFingerprint = "0123456789ABCDEF" // TODO: get legal hold client fingerprint
                )
            }
        }
    }

    fun acceptClicked() {
        state.ifVisible {
            state = it.copy(acceptEnabled = false, loading = true)
            // the accept button is enabled if the password is valid, this check is for safety only
            validatePassword(it.password.text).let { validatePasswordResult ->
                state = when (validatePasswordResult.isValid) {
                    false -> it.copy(loading = false, error = LegalHoldRequestedError.InvalidCredentialsError)
                    true -> LegalHoldRequestedState.Hidden // TODO: accept legal hold
                }
            }
        }
    }

    companion object {
        private const val TAG = "LegalHoldRequestedViewModel"
    }
}
