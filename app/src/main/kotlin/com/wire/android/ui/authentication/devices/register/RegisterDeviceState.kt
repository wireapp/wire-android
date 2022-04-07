package com.wire.android.ui.authentication.devices.register

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.CoreFailure

data class RegisterDeviceState(
    val password: TextFieldValue = TextFieldValue(""),
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: RegisterDeviceError = RegisterDeviceError.None
)

sealed class RegisterDeviceError {
    object None : RegisterDeviceError()
    object InvalidCredentialsError : RegisterDeviceError()
    data class GenericError(val coreFailure: CoreFailure) : RegisterDeviceError()
}
