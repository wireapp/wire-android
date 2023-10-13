package com.wire.android.ui.settings.devices.model

import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.kalium.logic.feature.e2ei.E2eiCertificate

data class DeviceDetailsState(
    val device: Device = Device(),
    val isCurrentDevice: Boolean = false,
    val removeDeviceDialogState: RemoveDeviceDialogState = RemoveDeviceDialogState.Hidden,
    val error: RemoveDeviceError = RemoveDeviceError.None,
    val fingerPrint: String? = null,
    val isSelfClient: Boolean = false,
    val userName: String? = null,
    val isE2eiCertificateActivated: Boolean = false,
    val e2eiCertificate: E2eiCertificate = E2eiCertificate()
)
