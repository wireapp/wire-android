package com.wire.android.ui.settings.devices.model

import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError

data class DeviceDetailsState(
    val device: Device,
    val isCurrentDevice: Boolean,
    val removeDeviceDialogState: RemoveDeviceDialogState = RemoveDeviceDialogState.Hidden,
    val error: RemoveDeviceError = RemoveDeviceError.None
)
