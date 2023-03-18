package com.wire.android.ui.settings.devices

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.CurrentAccount
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_DEVICE_ID
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.android.ui.settings.devices.model.DeviceDetailsState
import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.GetClientDetailsResult
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.ObserveOtherUserResult
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @CurrentAccount
    private val currentUserId: UserId,
    private val navigationManager: NavigationManager,
    private val deleteClient: DeleteClientUseCase,
    private val observeClientDetails: ObserveClientDetailsUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val fingerprintUseCase: ClientFingerprintUseCase,
    private val updateClientVerificationStatus: UpdateClientVerificationStatusUseCase,
    private val observeUserInfo: ObserveUserInfoUseCase
) : SavedStateViewModel(savedStateHandle) {

    private val deviceId: ClientId = ClientId(
        savedStateHandle.get<String>(EXTRA_DEVICE_ID)!!
    )

    private val userId: UserId =
        savedStateHandle.get<String>(EXTRA_USER_ID)!!.let { QualifiedIdMapperImpl(null).fromStringToQualifiedID(it) }

    var state: DeviceDetailsState by mutableStateOf(DeviceDetailsState(isSelfClient = isSelfClient))
        private set

    init {
        observeDeviceDetails()
        getClientFingerPrint()
        observeUserName()
    }

    private val isSelfClient: Boolean
        get() = currentUserId == userId

    private fun observeUserName() {
        if (!isSelfClient) {
            viewModelScope.launch {
                observeUserInfo(userId).collect { result ->
                    when (result) {
                        GetUserInfoResult.Failure -> {
                            /* no-op */
                        }
                        is GetUserInfoResult.Success -> state = state.copy(userName = result.otherUser.name)
                    }
                }
            }
        }
    }

    private fun getClientFingerPrint() {
        viewModelScope.launch {
            state = when (val result = fingerprintUseCase(userId, deviceId)) {
                is ClientFingerprintUseCase.Result.Failure -> state.copy(fingerPrint = null)
                is ClientFingerprintUseCase.Result.Success -> state.copy(fingerPrint = result.fingerprint.decodeToString())
            }
        }
    }

    private fun observeDeviceDetails() {
        viewModelScope.launch {
            observeClientDetails(userId, deviceId).collect { result ->
                when (result) {
                    is GetClientDetailsResult.Failure.Generic -> {
                        appLogger.e("Error getting self clients $result")
                        navigateBack()
                    }

                    is GetClientDetailsResult.Success -> {
                        state = state.copy(
                            device = Device(result.client),
                            isCurrentDevice = result.isCurrentClient,
                            isVerified = result.client.isVerified,
                            removeDeviceDialogState = RemoveDeviceDialogState.Hidden
                        )
                    }
                }
            }
        }
    }

    fun removeDevice() {
        viewModelScope.launch {
            val isPasswordRequired: Boolean = when (val passwordRequiredResult = isPasswordRequired()) {
                is IsPasswordRequiredUseCase.Result.Failure -> {
                    state = state.copy(error = RemoveDeviceError.GenericError(passwordRequiredResult.cause))
                    return@launch
                }

                is IsPasswordRequiredUseCase.Result.Success -> passwordRequiredResult.value
            }
            when (isPasswordRequired) {
                true -> showDeleteClientDialog(state.device)
                false -> deleteDevice(null)
            }
        }
    }

    private fun showDeleteClientDialog(device: Device) {
        state = device.let { RemoveDeviceDialogState.Visible(it) }.let {
            state.copy(
                error = RemoveDeviceError.None,
                removeDeviceDialogState = it
            )
        }
    }

    private suspend fun deleteDevice(password: String?) {
        when (val result = deleteClient(DeleteClientParam(password, deviceId))) {
            is DeleteClientResult.Failure.Generic -> state = state.copy(
                error = RemoveDeviceError.GenericError(result.genericFailure)
            )

            DeleteClientResult.Failure.InvalidCredentials -> state = state.copy(
                error = RemoveDeviceError.InvalidCredentialsError
            )

            DeleteClientResult.Failure.PasswordAuthRequired -> showDeleteClientDialog(state.device)
            DeleteClientResult.Success -> navigateBack()
        }
    }

    fun onPasswordChange(newText: TextFieldValue) {
        updateStateIfDialogVisible {
            if (it.password == newText) {
                state
            } else {
                state.copy(
                    removeDeviceDialogState = it.copy(password = newText, removeEnabled = newText.text.isNotEmpty()),
                    error = RemoveDeviceError.None
                )
            }
        }
    }

    fun onRemoveConfirmed() {
        (state?.removeDeviceDialogState as? RemoveDeviceDialogState.Visible)?.let { dialogStateVisible ->
            updateStateIfDialogVisible {
                state.copy(
                    removeDeviceDialogState = it.copy(loading = true, removeEnabled = false)
                )
            }
            viewModelScope.launch {
                deleteDevice(dialogStateVisible.password.text)
                updateStateIfDialogVisible { state.copy(removeDeviceDialogState = it.copy(loading = false)) }
            }
        }
    }

    private fun updateStateIfDialogVisible(newValue: (RemoveDeviceDialogState.Visible) -> DeviceDetailsState) {
        if (state.removeDeviceDialogState is RemoveDeviceDialogState.Visible) {
            state = newValue(state.removeDeviceDialogState as RemoveDeviceDialogState.Visible)
        }
    }

    fun onUpdateVerificationStatus(status: Boolean) {
        viewModelScope.launch {
            updateClientVerificationStatus(userId, deviceId, status)
        }
    }

    fun onDialogDismissed() {
        state = state.copy(removeDeviceDialogState = RemoveDeviceDialogState.Hidden)
    }

    fun clearDeleteClientError() {
        state = state.copy(error = RemoveDeviceError.None)
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.SelfDevices.getRouteWithArgs(), BackStackMode.UPDATE_EXISTED))
        }
    }
}
