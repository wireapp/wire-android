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
package com.wire.android.ui.home.appLock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.sha256
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SetLockScreenViewModel @Inject constructor(
    private val validatePassword: ValidatePasswordUseCase,
    private val globalDataStore: GlobalDataStore,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    var state: SetLockCodeViewState by mutableStateOf(SetLockCodeViewState())
        private set

    fun onPasswordChanged(password: TextFieldValue) {
        state = state.copy(
            password = password
        )
        state = if (validatePassword(password.text)) {
            state.copy(
                continueEnabled = true,
                isPasswordValid = true
            )
        } else {
            state.copy(
                isPasswordValid = false
            )
        }
    }

    fun onContinue() {
        state = state.copy(continueEnabled = false)
        // the continue button is enabled iff the password is valid
        // this check is for safety only
        if (!validatePassword(state.password.text)) {
            state = state.copy(isPasswordValid = false)
        } else {
            viewModelScope.launch {
                withContext(dispatchers.io()) {
                    globalDataStore.setAppLockPasscode(state.password.text.sha256())
                }
                withContext(dispatchers.main()) {
                    state = state.copy(done = true)
                }
            }
        }
    }
}
