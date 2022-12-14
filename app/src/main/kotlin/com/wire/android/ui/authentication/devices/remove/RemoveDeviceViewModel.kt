package com.wire.android.ui.authentication.devices.remove

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.kalium.logic.feature.client.SelfClientsUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions", "LongParameterList")
class RemoveDeviceViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val selfClientsUseCase: SelfClientsUseCase,
    private val deleteClientUseCase: DeleteClientUseCase,
    private val registerClientUseCase: GetOrRegisterClientUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val userDataStore: UserDataStore,
    private val getSessions: GetSessionsUseCase,
    private val currentSession: CurrentSessionUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val switchAccount: AccountSwitchUseCase
) : ViewModel() {

    var state: RemoveDeviceState by mutableStateOf(
        RemoveDeviceState(deviceList = listOf(), removeDeviceDialogState = RemoveDeviceDialogState.Hidden, isLoadingClientsList = true)
    )
        private set

    init {
        loadClientsList()
        checkNumberOfSessions()
    }

    fun loadClientsList() {
        viewModelScope.launch {
            state = state.copy(isLoadingClientsList = true)
            val selfClientsResult = selfClientsUseCase()
            if (selfClientsResult is SelfClientsResult.Success)
                state = state.copy(
                    isLoadingClientsList = false,
                    deviceList = selfClientsResult.clients.filter { it.type == ClientType.Permanent }.map { Device(it) },
                    removeDeviceDialogState = RemoveDeviceDialogState.Hidden
                )
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

    fun onBackButtonClicked() {
        if (!state.isFirstAccount) {
            state = state.copy(showCancelLoginDialog = true)
        }
    }

    fun onProceedLoginClicked() {
        state = state.copy(showCancelLoginDialog = false)
    }

    fun onCancelLoginClicked() {
        state = state.copy(showCancelLoginDialog = false)
        viewModelScope.launch {
            currentSession().let {
                when (it) {
                    is CurrentSessionResult.Success -> {
                        deleteSession(it.accountInfo.userId)
                    }
                    is CurrentSessionResult.Failure.Generic -> {
                        appLogger.e("failed to delete session")
                    }
                    CurrentSessionResult.Failure.SessionNotFound -> {
                        appLogger.e("session not found")

                    }
                }
            }
        }.invokeOnCompletion {
            viewModelScope.launch {
                switchAccount(
                    SwitchAccountParam.SwitchToNextAccountOrWelcome
                )
            }
        }
    }

    private fun checkNumberOfSessions() {
        viewModelScope.launch {
            getSessions().let {
                when (it) {
                    is GetAllSessionsResult.Success -> {
                        it.sessions[0].userId
                        state = if (it.sessions.filterIsInstance<AccountInfo.Valid>().size > 1) {
                            state.copy(isFirstAccount = false)
                        } else {
                            state.copy(isFirstAccount = true)
                        }
                    }
                    is GetAllSessionsResult.Failure.Generic -> {}
                    GetAllSessionsResult.Failure.NoSessionFound -> {
                        state = state.copy(isFirstAccount = true)
                    }
                }
            }
        }
    }

    fun onItemClicked(device: Device) {
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
        registerClientUseCase(
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
        when (val deleteResult = deleteClientUseCase(DeleteClientParam(password, device.clientId))) {
            is DeleteClientResult.Failure.Generic -> {
                state = state.copy(error = RemoveDeviceError.GenericError(deleteResult.genericFailure))
            }
            DeleteClientResult.Failure.InvalidCredentials -> state = state.copy(error = RemoveDeviceError.InvalidCredentialsError)
            DeleteClientResult.Failure.PasswordAuthRequired -> showDeleteClientDialog(device)
            DeleteClientResult.Success -> {
                // this delay is only a work around because the backend is not updating the list of clients immediately
                // TODO(revert me): remove the delay once the server side bug is fixed
                delay(REGISTER_CLIENT_AFTER_DELETE_DELAY)
                registerClient(password)
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
        if (userDataStore.initialSyncCompleted.first())
            navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
        else
            navigationManager.navigate(NavigationCommand(NavigationItem.InitialSync.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))

    private companion object {
        const val REGISTER_CLIENT_AFTER_DELETE_DELAY = 2000L
    }
}
