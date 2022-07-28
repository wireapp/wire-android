package com.wire.android.ui.authentication.devices.remove

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationItem
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.util.WillNeverOccurError
import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.kalium.logic.feature.client.SelfClientsUseCase
import com.wire.kalium.logic.feature.session.RegisterTokenResult
import com.wire.kalium.logic.feature.session.RegisterTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoveDeviceViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val selfClientsUseCase: SelfClientsUseCase,
    private val deleteClientUseCase: DeleteClientUseCase,
    private val registerClientUseCase: RegisterClientUseCase,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val pushTokenUseCase: RegisterTokenUseCase
) : ViewModel() {

    var state: RemoveDeviceState by mutableStateOf(
        RemoveDeviceState.Success(deviceList = listOf(), removeDeviceDialogState = RemoveDeviceDialogState.Hidden)
    )
        private set

    init {
        viewModelScope.launch {
            state = RemoveDeviceState.Loading
            val selfClientsResult = selfClientsUseCase()
            if (selfClientsResult is SelfClientsResult.Success)
                state = RemoveDeviceState.Success(
                    deviceList = selfClientsResult.clients.map { Device(it) },
                    removeDeviceDialogState = RemoveDeviceDialogState.Hidden
                )
        }
    }

    fun onPasswordChange(newText: TextFieldValue) {
        updateStateIfDialogVisible {
            if (it.password == newText) it
            else it.copy(password = newText, error = RemoveDeviceError.None, removeEnabled = newText.text.isNotEmpty())
        }
    }

    fun onDialogDismissed() {
        updateStateIfDialogVisible { RemoveDeviceDialogState.Hidden }
    }

    fun clearDeleteClientError() {
        updateStateIfDialogVisible { it.copy(error = RemoveDeviceError.None) }
    }

    fun onItemClicked(device: Device) {
        tryToDeleteOrShowPasswordDialog(device)
    }

    private fun tryToDeleteOrShowPasswordDialog(device: Device) {
        // try delete with no password (will success only for SSO accounts)
        viewModelScope.launch(Dispatchers.Main) {
            when (val deleteResult = deleteClientUseCase(DeleteClientParam(null, device.clientId))) {
                DeleteClientResult.Success -> registerClientUseCase(
                    RegisterClientUseCase.RegisterClientParam(null, null)
                ).also { result ->
                    when (result) {
                        is RegisterClientResult.Failure.PasswordAuthRequired -> updateStateIfSuccess {
                            it.copy(
                                removeDeviceDialogState = RemoveDeviceDialogState.Visible(
                                    device = device
                                )
                            )
                        }
                        is RegisterClientResult.Failure.Generic -> state = RemoveDeviceState.Error(result.genericFailure)
                        RegisterClientResult.Failure.InvalidCredentials ->
                            throw WillNeverOccurError(
                                "RemoveDeviceViewModel: " +
                                        "wrong password error when registering new client for accounts without password"
                            )
                        RegisterClientResult.Failure.TooManyClients ->
                            throw WillNeverOccurError(
                                "RemoveDeviceViewModel: " +
                                        "TooManyClients error when registering a new client directly after deleting one of the old clients"
                            )
                        is RegisterClientResult.Success -> {
                            registerPushToken(result.client.id)
                            navigateToConvScreen()
                        }
                    }
                }
                is DeleteClientResult.Failure.Generic -> state = RemoveDeviceState.Error(deleteResult.genericFailure)
                DeleteClientResult.Failure.InvalidCredentials -> showDeleteClientDialog(device)
                DeleteClientResult.Failure.PasswordAuthRequired -> showDeleteClientDialog(device)
            }
        }
    }

    fun onRemoveConfirmed(hideKeyboard: () -> Unit) {
        (state as? RemoveDeviceState.Success)?.let {
            (it.removeDeviceDialogState as? RemoveDeviceDialogState.Visible)?.let { dialogStateVisible ->
                updateStateIfDialogVisible { it.copy(loading = true, removeEnabled = false) }
                viewModelScope.launch {
                    val deleteClientParam = DeleteClientParam(dialogStateVisible.password.text, dialogStateVisible.device.clientId)
                    val deleteClientResult = deleteClientUseCase(deleteClientParam)
                    val removeDeviceError =
                        if (deleteClientResult is DeleteClientResult.Success)
                            if (!validatePasswordUseCase(dialogStateVisible.password.text)) RemoveDeviceError.InvalidCredentialsError
                            else {
                                registerClientUseCase(
                                    RegisterClientUseCase.RegisterClientParam(
                                        password = dialogStateVisible.password.text,
                                        capabilities = null,
                                    )
                                ).let { registerClientResult ->
                                    if (registerClientResult is RegisterClientResult.Success)
                                        registerPushToken(registerClientResult.client.id)
                                    registerClientResult.toRemoveDeviceError()
                                }
                            }
                        else
                            deleteClientResult.toRemoveDeviceError()
                    updateStateIfDialogVisible { it.copy(loading = false, error = removeDeviceError) }
                    if (removeDeviceError is RemoveDeviceError.None) {
                        hideKeyboard()
                        navigateToConvScreen()
                    }
                }
            }
        }
    }

    private fun showDeleteClientDialog(device: Device) {
        updateStateIfSuccess {
            it.copy(
                removeDeviceDialogState = RemoveDeviceDialogState.Visible(
                    device = device
                )
            )
        }
    }

    private suspend fun registerPushToken(clientId: ClientId) {
        pushTokenUseCase(BuildConfig.SENDER_ID, clientId).let { registerTokenResult ->
            when (registerTokenResult) {
                is RegisterTokenResult.Success ->
                    appLogger.i("PushToken Registered Successfully")
                is RegisterTokenResult.Failure ->
                    //TODO: handle failure in settings to allow the user to retry tokenRegistration
                    appLogger.i("PushToken Registration Failed: $registerTokenResult")
            }
        }
    }

    private fun DeleteClientResult.toRemoveDeviceError(): RemoveDeviceError =
        when (this) {
            is DeleteClientResult.Failure.Generic -> RemoveDeviceError.GenericError(this.genericFailure)
            DeleteClientResult.Failure.InvalidCredentials -> RemoveDeviceError.InvalidCredentialsError
            DeleteClientResult.Failure.PasswordAuthRequired -> RemoveDeviceError.PasswordRequired
            DeleteClientResult.Success -> RemoveDeviceError.None
        }

    private fun RegisterClientResult.toRemoveDeviceError(): RemoveDeviceError =
        when (this) {
            is RegisterClientResult.Failure.Generic -> RemoveDeviceError.GenericError(this.genericFailure)
            is RegisterClientResult.Failure.InvalidCredentials -> RemoveDeviceError.InvalidCredentialsError
            is RegisterClientResult.Failure.TooManyClients -> RemoveDeviceError.TooManyDevicesError
            RegisterClientResult.Failure.PasswordAuthRequired -> RemoveDeviceError.PasswordRequired
            is RegisterClientResult.Success -> RemoveDeviceError.None
        }


    private fun updateStateIfSuccess(newValue: (RemoveDeviceState.Success) -> RemoveDeviceState) =
        (state as? RemoveDeviceState.Success)?.let { state = newValue(it) }

    private fun updateStateIfDialogVisible(newValue: (RemoveDeviceDialogState.Visible) -> RemoveDeviceDialogState) =
        updateStateIfSuccess { stateSuccess ->
            if (stateSuccess.removeDeviceDialogState is RemoveDeviceDialogState.Visible)
                stateSuccess.copy(removeDeviceDialogState = newValue(stateSuccess.removeDeviceDialogState))
            else stateSuccess
        }

    private suspend fun navigateToConvScreen() =
        navigationManager.navigate(NavigationCommand(VoyagerNavigationItem.Home, BackStackMode.CLEAR_WHOLE))
}
