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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.analytics.FinalizeRegistrationAnalyticsMetadataUseCase
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.authentication.create.common.handle.HandleUpdateErrorState
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.logic.feature.auth.ValidateUserHandleResult
import com.wire.kalium.logic.feature.auth.ValidateUserHandleUseCase
import com.wire.kalium.logic.feature.user.SetUserHandleResult
import com.wire.kalium.logic.feature.user.SetUserHandleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAccountUsernameViewModel @Inject constructor(
    private val validateUserHandleUseCase: ValidateUserHandleUseCase,
    private val setUserHandleUseCase: SetUserHandleUseCase,
    private val anonymousAnalyticsManager: AnonymousAnalyticsManager,
    private val finalizeRegistrationAnalyticsMetadata: FinalizeRegistrationAnalyticsMetadataUseCase
) : ViewModel() {

    val textState: TextFieldState = TextFieldState()
    var state: CreateAccountUsernameViewState by mutableStateOf(CreateAccountUsernameViewState(continueEnabled = false))
        private set

    init {
        viewModelScope.launch {
            anonymousAnalyticsManager.sendEvent(AnalyticsEvent.RegistrationPersonalAccount.Username)
            textState.textAsFlow()
                .dropWhile { it.isEmpty() } // ignore first empty value to not show the error before the user typed anything
                .collectLatest { newHandle ->
                    validateUserHandleUseCase(newHandle.toString()).let { validateResult ->
                        state = when (validateResult) {
                            is ValidateUserHandleResult.Valid -> state.copy(
                                error = HandleUpdateErrorState.None,
                                continueEnabled = !state.loading,
                            )

                            is ValidateUserHandleResult.Invalid -> state.copy(
                                error = HandleUpdateErrorState.TextFieldError.UsernameInvalidError,
                                continueEnabled = false,
                            )
                        }
                    }
                }
        }
    }

    fun onErrorDismiss() {
        state = state.copy(error = HandleUpdateErrorState.None)
    }

    fun onContinue() {
        state = state.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            val usernameError = when (val result = setUserHandleUseCase(textState.text.toString().trim())) {
                is SetUserHandleResult.Failure.Generic -> HandleUpdateErrorState.DialogError.GenericError(result.error)
                SetUserHandleResult.Failure.HandleExists -> HandleUpdateErrorState.TextFieldError.UsernameTakenError
                SetUserHandleResult.Failure.InvalidHandle -> HandleUpdateErrorState.TextFieldError.UsernameInvalidError
                SetUserHandleResult.Success -> {
                    anonymousAnalyticsManager.sendEvent(AnalyticsEvent.RegistrationPersonalAccount.CreationCompleted)
                    finalizeRegistrationAnalyticsMetadata()
                    HandleUpdateErrorState.None
                }
            }
            state = state.copy(
                loading = false,
                continueEnabled = true,
                error = usernameError,
                success = usernameError is HandleUpdateErrorState.None,
            )
        }
    }
}
