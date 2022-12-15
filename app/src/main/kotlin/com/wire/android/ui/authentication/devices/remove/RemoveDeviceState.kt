package com.wire.android.ui.authentication.devices.remove

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.kalium.logic.CoreFailure

data class RemoveDeviceState(
    val deviceList: List<Device>,
    val removeDeviceDialogState: RemoveDeviceDialogState = RemoveDeviceDialogState.Hidden,
    val isLoadingClientsList: Boolean,
    val error: RemoveDeviceError = RemoveDeviceError.None,
    val isFirstAccount: Boolean = true,
    val showCancelLoginDialog: Boolean = false
)

sealed class RemoveDeviceDialogState {
    object Hidden : RemoveDeviceDialogState()
    data class Visible(
        val device: Device,
        val password: TextFieldValue = TextFieldValue(""),
        val loading: Boolean = false,
        val removeEnabled: Boolean = false
    ) : RemoveDeviceDialogState()
}

sealed class RemoveDeviceError {
    object None : RemoveDeviceError()
    object InvalidCredentialsError : RemoveDeviceError()
    object TooManyDevicesError : RemoveDeviceError()
    object PasswordRequired : RemoveDeviceError()
    data class GenericError(val coreFailure: CoreFailure) : RemoveDeviceError()
}
