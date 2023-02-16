package com.wire.android.ui.settings.devices.model

import com.wire.android.ui.authentication.devices.model.Device

data class DeviceDetailsState(
    val device: Device?,
    val isCurrentDevice: Boolean
)
