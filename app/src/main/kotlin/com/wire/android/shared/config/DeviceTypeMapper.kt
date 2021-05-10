package com.wire.android.shared.config

import com.wire.android.core.config.DeviceType
import com.wire.android.core.config.Phone
import com.wire.android.core.config.Tablet

class DeviceTypeMapper {

    fun toStringValue(type: DeviceType): String =
        when (type) {
            Tablet -> "tablet"
            Phone -> "phone"
        }
}
