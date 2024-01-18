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

package com.wire.android.ui.authentication.devices.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class RegisterDeviceViewModel @Inject constructor(
    private val registerClientUseCase: GetOrRegisterClientUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val userDataStore: UserDataStore,
) : ViewModel() {

    var state: RegisterDeviceState by mutableStateOf(RegisterDeviceState())
        private set

    init {
        runBlocking {
            state = state.copy(flowState = RegisterDeviceFlowState.Loading)
            isPasswordRequired().let {
                state = state.copy(flowState = RegisterDeviceFlowState.Default)
                when (it) {
                    is IsPasswordRequiredUseCase.Result.Failure -> {
                        updateFlowState(RegisterDeviceFlowState.Error.GenericError(it.cause))
                    }

                    is IsPasswordRequiredUseCase.Result.Success -> {
                        if (!it.value) registerClient(null)
                    }
                }
            }
        }
    }

    fun onPasswordChange(newText: TextFieldValue) {
        if (state.password != newText) {
            state = state.copy(password = newText, flowState = RegisterDeviceFlowState.Default, continueEnabled = newText.text.isNotEmpty())
        }
    }

    fun onErrorDismiss() {
        updateFlowState(RegisterDeviceFlowState.Default)
    }

    private suspend fun registerClient(password: String?) {
        state = state.copy(flowState = RegisterDeviceFlowState.Loading, continueEnabled = false)
        when (val registerDeviceResult = registerClientUseCase(
            RegisterClientUseCase.RegisterClientParam(
                password = password,
                capabilities = null,
            )
        )) {
            is RegisterClientResult.Failure.TooManyClients ->
                updateFlowState(RegisterDeviceFlowState.TooManyDevices)
            is RegisterClientResult.Success ->
                updateFlowState(RegisterDeviceFlowState.Success(userDataStore.initialSyncCompleted.first(), false, registerDeviceResult.client.id ))
            is RegisterClientResult.E2EICertificateRequired ->
                updateFlowState(RegisterDeviceFlowState.Success(userDataStore.initialSyncCompleted.first(), true, registerDeviceResult.client.id, registerDeviceResult.userId))
            is RegisterClientResult.Failure.Generic -> state = state.copy(
                continueEnabled = true,
                flowState = RegisterDeviceFlowState.Error.GenericError(registerDeviceResult.genericFailure)
            )

            is RegisterClientResult.Failure.InvalidCredentials -> state = state.copy(
                continueEnabled = true,
                flowState = RegisterDeviceFlowState.Error.InvalidCredentialsError
            )

            is RegisterClientResult.Failure.PasswordAuthRequired -> { /* app is already waiting for the user to enter the password */ }
        }
    }

    fun onContinue() {
        viewModelScope.launch {
            registerClient(state.password.text)
        }
    }

    private fun updateFlowState(flowState: RegisterDeviceFlowState) {
        state = state.copy(flowState = flowState)
    }
}
