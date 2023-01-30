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
 *
 *
 */

package com.wire.android.ui.authentication.devices.remove

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.kalium.logic.feature.client.SelfClientsUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class RemoveDeviceViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val selfClients: SelfClientsUseCase,
    private val deleteClient: DeleteClientUseCase,
    private val registerClient: GetOrRegisterClientUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val userDataStore: UserDataStore
) : ViewModel() {

    var state: RemoveDeviceState by mutableStateOf(
        RemoveDeviceState(
            deviceList = persistentListOf(),
            removeDeviceDialogState = RemoveDeviceDialogState.Hidden,
            isLoadingClientsList = true,
            error = RemoveDeviceError.None,
            currentDevice = null
        )
    )
        private set

    init {
        loadClientsList()
    }

    private fun loadClientsList() {
        viewModelScope.launch {
            state = state.copy(isLoadingClientsList = true)
            val selfClientsResult = selfClients()
            if (selfClientsResult is SelfClientsResult.Success) {
                state = state.copy(
                    isLoadingClientsList = false,
                    deviceList = selfClientsResult.clients
                        .filter { it.id != selfClientsResult.currentClientId && it.type == ClientType.Permanent }
                        .map { Device(it) }.toImmutableList(),
                    removeDeviceDialogState = RemoveDeviceDialogState.Hidden,
                    currentDevice = selfClientsResult.clients
                        .firstOrNull { it.id == selfClientsResult.currentClientId }?.let { Device(it) }
                )
            }
        }
    }

    fun onPasswordChange(newText: TextFieldValue) {
        updateStateIfDialogVisible {
            if (it.password == newText) state
            else state.copy(
                removeDeviceDialogState = it.copy(password = newText, removeEnabled = newText.text.isNotEmpty()),
                error = RemoveDeviceError.None
            )
        }
    }

    fun onDialogDismissed() {
        updateStateIfDialogVisible { state.copy(removeDeviceDialogState = RemoveDeviceDialogState.Hidden) }
    }

    fun clearDeleteClientError() {
        updateStateIfDialogVisible { state.copy(error = RemoveDeviceError.None) }
    }

    fun onItemClicked(device: Device, shouldRegisterClient: Boolean = true) {
        state = state.copy(shouldRegisterClient = shouldRegisterClient)
        viewModelScope.launch {
            val isPasswordRequired: Boolean = when (val passwordRequiredResult = isPasswordRequired()) {
                is IsPasswordRequiredUseCase.Result.Failure -> {
                    updateStateIfDialogVisible { state.copy(error = RemoveDeviceError.GenericError(passwordRequiredResult.cause)) }
                    return@launch
                }
                is IsPasswordRequiredUseCase.Result.Success -> passwordRequiredResult.value
            }
            when (isPasswordRequired) {
                true -> {
                    // show dialog for the user to enter the password
                    showDeleteClientDialog(device)
                }
                // no password needed so we can delete the device and register a client
                false -> deleteClient(null, device)
            }
        }
    }

    private suspend fun registerClient(password: String?) {
        registerClient(
            RegisterClientUseCase.RegisterClientParam(password, null)
        ).also { result ->
            when (result) {
                is RegisterClientResult.Failure.PasswordAuthRequired -> {
                    /* the check for password is done before this function is called */
                }
                is RegisterClientResult.Failure.Generic -> state = state.copy(error = RemoveDeviceError.GenericError(result.genericFailure))
                RegisterClientResult.Failure.InvalidCredentials -> state = state.copy(error = RemoveDeviceError.InvalidCredentialsError)
                RegisterClientResult.Failure.TooManyClients -> loadClientsList()
                is RegisterClientResult.Success -> {
                    navigateAfterRegisterClientSuccess()
                }
            }
        }
    }

    private suspend fun deleteClient(password: String?, device: Device) {
        when (val deleteResult = deleteClient(DeleteClientParam(password, device.clientId))) {
            is DeleteClientResult.Failure.Generic -> {
                state = state.copy(error = RemoveDeviceError.GenericError(deleteResult.genericFailure))
            }
            DeleteClientResult.Failure.InvalidCredentials -> state = state.copy(error = RemoveDeviceError.InvalidCredentialsError)
            DeleteClientResult.Failure.PasswordAuthRequired -> showDeleteClientDialog(device)
            DeleteClientResult.Success -> {
                // this delay is only a work around because the backend is not updating the list of clients immediately
                // TODO(revert me): remove the delay once the server side bug is fixed
                delay(REGISTER_CLIENT_AFTER_DELETE_DELAY)
                if (state.shouldRegisterClient) registerClient(password)
                else navigationManager.navigateBack()
            }
        }
    }

    fun onRemoveConfirmed() {
        (state.removeDeviceDialogState as? RemoveDeviceDialogState.Visible)?.let { dialogStateVisible ->
            updateStateIfDialogVisible { state.copy(removeDeviceDialogState = it.copy(loading = true, removeEnabled = false)) }
            viewModelScope.launch {
                deleteClient(dialogStateVisible.password.text, dialogStateVisible.device)
                updateStateIfDialogVisible { state.copy(removeDeviceDialogState = it.copy(loading = false)) }
            }
        }
    }

    private fun showDeleteClientDialog(device: Device) {
        state = state.copy(
            error = RemoveDeviceError.None,
            removeDeviceDialogState = RemoveDeviceDialogState.Visible(
                device = device
            )
        )
    }

    private fun updateStateIfDialogVisible(newValue: (RemoveDeviceDialogState.Visible) -> RemoveDeviceState) {
        if (state.removeDeviceDialogState is RemoveDeviceDialogState.Visible) {
            state = newValue(state.removeDeviceDialogState as RemoveDeviceDialogState.Visible)
        }
    }

    @VisibleForTesting
    private suspend fun navigateAfterRegisterClientSuccess() =
        if (userDataStore.initialSyncCompleted.first()) {
            navigationManager.navigate(
                NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE)
            )
        } else {
            navigationManager.navigate(
                NavigationCommand(NavigationItem.InitialSync.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE)
            )
        }

    private companion object {
        const val REGISTER_CLIENT_AFTER_DELETE_DELAY = 2_000L
    }
}
