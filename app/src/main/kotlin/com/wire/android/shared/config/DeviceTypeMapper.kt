package com.wire.android.shared.config

import com.wire.android.core.config.DeviceClass
import com.wire.android.core.config.Phone
import com.wire.android.core.config.Tablet

class DeviceTypeMapper {

    fun toStringValue(aClass: DeviceClass): String =
        when (aClass) {
            Tablet -> "tablet"
            Phone -> "phone"
        }
}
