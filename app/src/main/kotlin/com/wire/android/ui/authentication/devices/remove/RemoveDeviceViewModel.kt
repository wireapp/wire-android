package com.wire.android.ui.authentication.devices.remove

import androidx.compose.material.ExperimentalMaterialApi
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
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.kalium.logic.data.client.DeleteClientParam
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterialApi::class)
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
                    deviceList = selfClientsResult.clients.map {
                        Device(
                            name = it.label ?: it.model ?: "",
                            clientId = it.clientId,
                            registrationTime = it.registrationTime
                        )
                    },
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
        updateStateIfSuccess { it.copy(removeDeviceDialogState = RemoveDeviceDialogState.Visible(device = device)) }
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
                                        registerPushToken(registerClientResult.client.clientId.value)
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

    private suspend fun registerPushToken(clientId: String) {
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

    private fun DeleteClientResult.toRemoveDeviceError() =
        when (this) {
            is DeleteClientResult.Failure.Generic -> RemoveDeviceError.GenericError(this.genericFailure)
            DeleteClientResult.Failure.InvalidCredentials -> RemoveDeviceError.InvalidCredentialsError
            DeleteClientResult.Success -> RemoveDeviceError.None
        }

    private fun RegisterClientResult.toRemoveDeviceError() =
        when (this) {
            is RegisterClientResult.Failure.Generic -> RemoveDeviceError.GenericError(this.genericFailure)
            is RegisterClientResult.Failure.InvalidCredentials -> RemoveDeviceError.InvalidCredentialsError
            is RegisterClientResult.Failure.TooManyClients -> RemoveDeviceError.TooManyDevicesError
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
        navigationManager.navigate(NavigationCommand(NavigationItem.Home.getRouteWithArgs(), BackStackMode.CLEAR_WHOLE))
}
