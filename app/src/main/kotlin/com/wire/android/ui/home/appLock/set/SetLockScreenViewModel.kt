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
package com.wire.android.ui.home.appLock.set

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.AppLockSource
import com.wire.android.feature.ObserveAppLockConfigUseCase
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.applock.MarkTeamAppLockStatusAsNotifiedUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppLockEditableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SetLockScreenViewModel @Inject constructor(
    private val validatePassword: ValidatePasswordUseCase,
    private val globalDataStore: GlobalDataStore,
    private val dispatchers: DispatcherProvider,
    private val observeAppLockConfig: ObserveAppLockConfigUseCase,
    private val observeIsAppLockEditable: ObserveIsAppLockEditableUseCase,
    private val markTeamAppLockStatusAsNotified: MarkTeamAppLockStatusAsNotifiedUseCase
) : ViewModel() {

    val passwordTextState: TextFieldState = TextFieldState()
    var state: SetLockCodeViewState by mutableStateOf(SetLockCodeViewState())
        private set

    init {
        viewModelScope.launch {
            passwordTextState.textAsFlow().collect {
                state = state.copy(passwordValidation = validatePassword(it.toString()))
            }
        }
        viewModelScope.launch {
            combine(
                observeAppLockConfig(),
                observeIsAppLockEditable()
            ) { config, isEditable ->
                state.copy(
                    timeout = config.timeout,
                    isEditable = isEditable
                )
            }.collectLatest { state = it }
        }
    }

    fun onContinue() {
        state = state.copy(loading = true)
        // the continue button is enabled iff the password is valid
        // this check is for safety only
        validatePassword(passwordTextState.text.toString()).let {
            state = state.copy(passwordValidation = it)
            if (it.isValid) {
                viewModelScope.launch {
                    withContext(dispatchers.io()) {
                        with(globalDataStore) {
                            val source = if (state.isEditable) {
                                AppLockSource.Manual
                            } else {
                                AppLockSource.TeamEnforced
                            }

                            setUserAppLock(passwordTextState.text.toString(), source)

                            // TODO(bug): this does not take into account which account enforced the app lock
                            markTeamAppLockStatusAsNotified()
                        }
                    }
                    withContext(dispatchers.main()) {
                        state = state.copy(done = true)
                    }
                }
            }
            state = state.copy(loading = false)
        }
    }
}
