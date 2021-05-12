package com.wire.android.shared.config

import com.wire.android.core.config.DeviceType
import com.wire.android.core.config.LegalHold
import com.wire.android.core.config.Permanent
import com.wire.android.core.config.Temporary

class DeviceTypeMapper {

    fun toStringValue(type: DeviceType) : String =
        when (type) {
            Permanent -> "permanent"
            Temporary -> "temporary"
            LegalHold -> "legalhold"
            else -> "unknown"
        }
}
