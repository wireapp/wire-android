package com.wire.android.shared.config

import com.wire.android.core.config.DeviceClass
import com.wire.android.core.config.Phone
import com.wire.android.core.config.Tablet

class DeviceClassMapper {

    fun toStringValue(deviceClass: DeviceClass): String =
        when (deviceClass) {
            Tablet -> "tablet"
            Phone -> "phone"
        }
}
