package com.wire.android.ui.settings.devices.model

import com.wire.android.ui.authentication.devices.model.Device

data class SelfDevicesState (
    val currentDevice: Device?,
    val deviceList: List<Device>,
    val isLoadingClientsList: Boolean
)
