package com.wire.android.ui.settings.devices.model

import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError

data class DeviceDetailsState(
    val device: Device = Device(),
    val isCurrentDevice: Boolean = false,
    val removeDeviceDialogState: RemoveDeviceDialogState = RemoveDeviceDialogState.Hidden,
    val error: RemoveDeviceError = RemoveDeviceError.None,
    val isVerified: Boolean = false,
    val fingerPrint: String? = null,
    val isSelfClient: Boolean = false,
    val userName: String? = null
)
