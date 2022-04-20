package com.wire.android.ui.authentication.devices.remove

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.kalium.logic.CoreFailure

sealed class RemoveDeviceState {
    data class Success(
        val deviceList: List<Device>,
        val removeDeviceDialogState: RemoveDeviceDialogState
    ) : RemoveDeviceState()

    object Loading : RemoveDeviceState()
    data class Error(val coreFailure: CoreFailure) : RemoveDeviceState()
}

sealed class RemoveDeviceDialogState {
    object Hidden : RemoveDeviceDialogState()
    data class Visible(
        val device: Device,
        val password: TextFieldValue = TextFieldValue(""),
        val loading: Boolean = false,
        val removeEnabled: Boolean = false,
        val hideKeyboard: Boolean = false,
        val error: RemoveDeviceError = RemoveDeviceError.None
    ) : RemoveDeviceDialogState()
}

sealed class RemoveDeviceError {
    object None : RemoveDeviceError()
    object InvalidCredentialsError : RemoveDeviceError()
    object TooManyDevicesError : RemoveDeviceError()
    data class GenericError(val coreFailure: CoreFailure) : RemoveDeviceError()
}
