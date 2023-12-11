package com.wire.android.ui.settings.devices

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.e2ei.GetE2EICertificateUseCase
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.android.ui.navArgs
import com.wire.android.ui.settings.devices.model.DeviceDetailsState
import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.GetClientDetailsResult
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.Result
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.logic.feature.e2ei.usecase.GetE2EICertificateUseCaseResult
import com.wire.kalium.logic.feature.e2ei.usecase.GetE2eiCertificateUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import com.wire.kalium.logic.functional.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @CurrentAccount
    private val currentUserId: UserId,
    private val deleteClient: DeleteClientUseCase,
    private val observeClientDetails: ObserveClientDetailsUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val fingerprintUseCase: ClientFingerprintUseCase,
    private val updateClientVerificationStatus: UpdateClientVerificationStatusUseCase,
    private val observeUserInfo: ObserveUserInfoUseCase,
    private val e2eiCertificate: GetE2eiCertificateUseCase,
    private val enrolE2EICertificateUseCase: GetE2EICertificateUseCase
) : SavedStateViewModel(savedStateHandle) {

    private val deviceDetailsNavArgs: DeviceDetailsNavArgs = savedStateHandle.navArgs()
    private val deviceId: ClientId = deviceDetailsNavArgs.clientId
    private val userId: UserId = deviceDetailsNavArgs.userId

    var state: DeviceDetailsState by mutableStateOf(DeviceDetailsState(isSelfClient = isSelfClient))
        private set

    init {
        observeDeviceDetails()
        getClientFingerPrint()
        observeUserName()
        getE2eiCertificate()
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

                        is GetUserInfoResult.Success -> state =
                            state.copy(userName = result.otherUser.name)
                    }
                }
            }
        }
    }

    private fun getE2eiCertificate() {
        viewModelScope.launch {
            val certificate = e2eiCertificate(deviceId)
            state = if (certificate is GetE2EICertificateUseCaseResult.Success) {
                println("cyka getting cert success")
                state.copy(
                    isE2eiCertificateActivated = true,
                    e2eiCertificate = certificate.certificate,
                    isLoadingCertificate = false
                )
            } else {
                println("cyka getting cert success")
                state.copy(isE2eiCertificateActivated = false, isLoadingCertificate = false)
            }
        }
    }

    fun enrollE2eiCertificate(context: Context) {
        state = state.copy(isLoadingCertificate = true)
        enrolE2EICertificateUseCase(context) { result ->
            result.fold({
                println("cyka error0 $it")
                state = state.copy(
                    isLoadingCertificate = false,
                    isE2EICertificateEnrollError = true
                )
            }, {
                if (it is E2EIEnrollmentResult.Finalized) {
                    println("cyka Finalized, getting cert")
                    getE2eiCertificate()
                    state = state.copy(isE2EICertificateEnrollSuccess = true)
                } else if (it is E2EIEnrollmentResult.Failed) {
                    println("cyka error1 $it")
                    state = state.copy(
                        isLoadingCertificate = false,
                        isE2EICertificateEnrollError = true
                    )
                } else {
                    println("cyka other: $it")
                }
            })
        }
    }

    private fun getClientFingerPrint() {
        viewModelScope.launch {
            state = when (val result = fingerprintUseCase(userId, deviceId)) {
                is Result.Failure -> state.copy(fingerPrint = null)
                is Result.Success -> state.copy(fingerPrint = result.fingerprint.decodeToString())
            }
        }
    }

    private fun observeDeviceDetails() {
        viewModelScope.launch {
            observeClientDetails(userId, deviceId).collect { result ->
                when (result) {
                    is GetClientDetailsResult.Failure.Generic -> {
                        appLogger.e("Error getting self clients $result")
                        state = state.copy(error = RemoveDeviceError.InitError)
                    }

                    is GetClientDetailsResult.Success -> {
                        state = state.copy(
                            device = Device(result.client),
                            isCurrentDevice = result.isCurrentClient,
                            removeDeviceDialogState = RemoveDeviceDialogState.Hidden
                        )
                    }
                }
            }
        }
    }

    fun removeDevice(onSuccess: () -> Unit) {
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
                false -> deleteDevice(null, onSuccess)
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

    private suspend fun deleteDevice(password: String?, onSuccess: () -> Unit) {
        when (val result = deleteClient(DeleteClientParam(password, deviceId))) {
            is DeleteClientResult.Failure.Generic -> state = state.copy(
                error = RemoveDeviceError.GenericError(result.genericFailure)
            )

            DeleteClientResult.Failure.InvalidCredentials -> state = state.copy(
                error = RemoveDeviceError.InvalidCredentialsError
            )

            DeleteClientResult.Failure.PasswordAuthRequired -> showDeleteClientDialog(state.device)
            DeleteClientResult.Success -> onSuccess()
        }
    }

    fun onPasswordChange(newText: TextFieldValue) {
        updateStateIfDialogVisible {
            if (it.password == newText) {
                state
            } else {
                state.copy(
                    removeDeviceDialogState = it.copy(
                        password = newText,
                        removeEnabled = newText.text.isNotEmpty()
                    ),
                    error = RemoveDeviceError.None
                )
            }
        }
    }

    fun onRemoveConfirmed(onSuccess: () -> Unit) {
        (state.removeDeviceDialogState as? RemoveDeviceDialogState.Visible)?.let { dialogStateVisible ->
            updateStateIfDialogVisible {
                state.copy(
                    removeDeviceDialogState = it.copy(loading = true, removeEnabled = false)
                )
            }
            viewModelScope.launch {
                deleteDevice(dialogStateVisible.password.text, onSuccess)
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

    fun hideEnrollE2EICertificateError() {
        state = state.copy(isE2EICertificateEnrollError = false)
    }

    fun hideEnrollE2EICertificateSuccess() {
        state = state.copy(isE2EICertificateEnrollSuccess = false)
    }
}
