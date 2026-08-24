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

package com.wire.android.ui.authentication.devices.remove

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.authentication.devices.register.PasswordRequirement
import com.wire.android.ui.authentication.devices.register.RegisterDeviceRequest
import com.wire.android.ui.authentication.devices.register.RegisterDeviceResult
import com.wire.android.ui.authentication.devices.register.RequestVerificationCodeResult
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class RemoveDeviceViewModel<DeviceT>(
    private val gateway: RemoveDeviceGateway<DeviceT>,
) : ActionsViewModel<RemoveDeviceViewAction>() {

    val passwordTextState: TextFieldState = TextFieldState()
    var state: RemoveDeviceAuthenticationState<DeviceT> by mutableStateOf(RemoveDeviceAuthenticationState())
        private set

    val secondFactorVerificationCodeTextState: TextFieldState = TextFieldState()
    var secondFactorVerificationCodeState: VerificationCodeState by mutableStateOf(VerificationCodeState())
        private set

    init {
        loadClientsList()
        observePasswordTextChanges()
        observeSecondFactorVerificationCodeChanges()
    }

    private fun observePasswordTextChanges() {
        viewModelScope.launch {
            passwordTextState.textAsFlow().distinctUntilChanged().collectLatest { newPassword ->
                updateStateIfDialogVisible { visible ->
                    state.copy(
                        removeDeviceDialogState = visible.copy(removeEnabled = newPassword.isNotEmpty()),
                        error = RemoveDeviceAuthenticationError.None,
                    )
                }
            }
        }
    }

    private fun observeSecondFactorVerificationCodeChanges() {
        viewModelScope.launch {
            secondFactorVerificationCodeTextState.textAsFlow().collectLatest { code ->
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCurrentCodeInvalid = false)
                if (code.length == VerificationCodeState.DEFAULT_VERIFICATION_CODE_LENGTH) {
                    state = state.copy(
                        is2FAInProgress = true,
                        removeDeviceDialogState = RemoveDeviceAuthenticationDialogState.Hidden,
                    )
                    registerClient(passwordTextState.text.toString(), code.toString())
                }
            }
        }
    }

    private fun loadClientsList() {
        viewModelScope.launch {
            state = state.copy(isLoadingClientsList = true)
            state = when (val result = gateway.fetchPermanentDevices()) {
                is FetchPermanentDevicesResult.Success -> state.copy(
                    isLoadingClientsList = false,
                    deviceList = result.devices,
                    removeDeviceDialogState = RemoveDeviceAuthenticationDialogState.Hidden,
                )

                is FetchPermanentDevicesResult.Failure -> state.copy(
                    isLoadingClientsList = false,
                    error = RemoveDeviceAuthenticationError.InitError,
                )
            }
        }
    }

    fun onDialogDismissed() {
        passwordTextState.clearText()
        updateStateIfDialogVisible {
            state.copy(removeDeviceDialogState = RemoveDeviceAuthenticationDialogState.Hidden)
        }
    }

    fun clearDeleteClientError() {
        state = state.copy(error = RemoveDeviceAuthenticationError.None)
    }

    fun retryFetch() {
        state = state.copy(isLoadingClientsList = true, error = RemoveDeviceAuthenticationError.None)
        loadClientsList()
    }

    fun onItemClicked(device: DeviceT) {
        viewModelScope.launch {
            when (val result = gateway.passwordRequirement()) {
                is PasswordRequirement.Failure -> {
                    state = state.copy(error = RemoveDeviceAuthenticationError.GenericError(result.failure))
                }

                PasswordRequirement.Required -> showDeleteClientDialog(device)
                PasswordRequirement.NotRequired -> deleteClient(null, device)
            }
        }
    }

    private suspend fun registerClient(password: String?, secondFactorVerificationCode: String? = null) {
        when (
            val result = gateway.registerClient(
                RegisterDeviceRequest(
                    password = password,
                    verificationCode = secondFactorVerificationCode,
                )
            )
        ) {
            RegisterDeviceResult.PasswordRequired -> {
                /* the password requirement is checked before registration */
            }

            is RegisterDeviceResult.Failure -> state = state.copy(
                error = RemoveDeviceAuthenticationError.GenericError(result.failure)
            )

            RegisterDeviceResult.MissingSecondFactor -> request2FACode()

            RegisterDeviceResult.InvalidSecondFactor -> {
                state = state.copy(is2FAInProgress = false)
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
                    isCodeInputNecessary = true,
                    isCurrentCodeInvalid = true,
                )
            }

            RegisterDeviceResult.InvalidCredentials -> state = state.copy(
                error = RemoveDeviceAuthenticationError.InvalidCredentialsError
            )

            RegisterDeviceResult.TooManyDevices -> loadClientsList()

            is RegisterDeviceResult.Success -> sendAction(
                OnComplete(
                    initialSyncCompleted = result.initialSyncCompleted,
                    isE2EIRequired = result.isE2EIRequired,
                )
            )
        }
    }

    private suspend fun deleteClient(password: String?, device: DeviceT) {
        when (val result = gateway.deleteDevice(password, device)) {
            is DeleteDeviceResult.Failure -> state = state.copy(
                error = RemoveDeviceAuthenticationError.GenericError(result.failure)
            )

            DeleteDeviceResult.InvalidCredentials -> state = state.copy(
                error = RemoveDeviceAuthenticationError.InvalidCredentialsError
            )

            DeleteDeviceResult.PasswordRequired -> showDeleteClientDialog(device)

            DeleteDeviceResult.Success -> {
                delay(REGISTER_CLIENT_AFTER_DELETE_DELAY_MILLIS)
                registerClient(password)
            }
        }
    }

    fun onRemoveConfirmed() {
        val visible = state.removeDeviceDialogState as? RemoveDeviceAuthenticationDialogState.Visible ?: return
        updateStateIfDialogVisible {
            state.copy(removeDeviceDialogState = it.copy(loading = true, removeEnabled = false))
        }
        viewModelScope.launch {
            deleteClient(passwordTextState.text.toString(), visible.device)
            updateStateIfDialogVisible {
                state.copy(removeDeviceDialogState = it.copy(loading = false))
            }
        }
    }

    private fun showDeleteClientDialog(device: DeviceT) {
        passwordTextState.clearText()
        state = state.copy(
            error = RemoveDeviceAuthenticationError.None,
            removeDeviceDialogState = RemoveDeviceAuthenticationDialogState.Visible(device),
        )
    }

    private fun updateStateIfDialogVisible(
        newValue: (
            RemoveDeviceAuthenticationDialogState.Visible<DeviceT>
        ) -> RemoveDeviceAuthenticationState<DeviceT>
    ) {
        val visible = state.removeDeviceDialogState as? RemoveDeviceAuthenticationDialogState.Visible ?: return
        state = newValue(visible)
    }

    fun onCodeResend() {
        viewModelScope.launch {
            request2FACode()
        }
    }

    fun onCodeVerificationBackPress() {
        secondFactorVerificationCodeTextState.clearText()
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
            isCodeInputNecessary = false,
            emailUsed = "",
        )
    }

    private suspend fun request2FACode() {
        when (val result = gateway.requestVerificationCode()) {
            is RequestVerificationCodeResult.Sent -> showVerificationCodeInput(result.email)
            is RequestVerificationCodeResult.TooManyRequests -> showVerificationCodeInput(result.email)
            RequestVerificationCodeResult.MissingEmail -> Unit
            is RequestVerificationCodeResult.Failure -> state = state.copy(
                error = RemoveDeviceAuthenticationError.GenericError(result.failure)
            )
        }
    }

    private fun showVerificationCodeInput(email: String) {
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
            isCodeInputNecessary = true,
            emailUsed = email,
        )
    }

    internal companion object {
        const val REGISTER_CLIENT_AFTER_DELETE_DELAY_MILLIS = 2000L
    }
}

sealed interface RemoveDeviceViewAction

data class OnComplete(
    val initialSyncCompleted: Boolean,
    val isE2EIRequired: Boolean,
) : RemoveDeviceViewAction
