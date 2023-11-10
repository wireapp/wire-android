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
package com.wire.android.ui.home.appLock.set

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.AppLockConfig
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.sha256
import com.wire.kalium.logic.feature.applock.MarkTeamAppLockStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SetLockScreenViewModel @Inject constructor(
    private val validatePassword: ValidatePasswordUseCase,
    private val globalDataStore: GlobalDataStore,
    private val dispatchers: DispatcherProvider,
    private val observeAppLockConfigUseCase: ObserveAppLockConfigUseCase,
    private val markTeamAppLockStatusAsNotified: MarkTeamAppLockStatusAsNotifiedUseCase
) : ViewModel() {

    var state: SetLockCodeViewState by mutableStateOf(SetLockCodeViewState())
        private set

    init {
        viewModelScope.launch {
            observeAppLockConfigUseCase()
                .collectLatest {
                    state = state.copy(
                        timeout = it.timeout,
                        isAppLockByUser = it !is AppLockConfig.EnforcedByTeam
                    )
                }
        }
    }

    fun onPasswordChanged(password: TextFieldValue) {
        state = state.copy(
            password = password
        )
        validatePassword(password.text).let {
            state = state.copy(
                continueEnabled = it.isValid,
                passwordValidation = it
            )
        }
    }

    fun onContinue() {
        state = state.copy(continueEnabled = false)
        // the continue button is enabled iff the password is valid
        // this check is for safety only
        validatePassword(state.password.text).let {
            state = state.copy(passwordValidation = it)
            if (it.isValid) {
                viewModelScope.launch {
                    withContext(dispatchers.io()) {
                        with(globalDataStore) {
                            if (state.isAppLockByUser) {
                                setUserAppLock(state.password.text.sha256())
                            } else {
                                setTeamAppLock(state.password.text.sha256())
                            }
                            markTeamAppLockStatusAsNotified()
                        }
                    }
                    withContext(dispatchers.main()) {
                        state = state.copy(done = true)
                    }
                }
            }
        }
    }
}
