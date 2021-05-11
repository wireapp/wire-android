package com.wire.android.feature.auth.client.datasource.mapper

import com.wire.android.feature.auth.client.datasource.DeviceType
import com.wire.android.feature.auth.client.datasource.LegalHold
import com.wire.android.feature.auth.client.datasource.Permanent
import com.wire.android.feature.auth.client.datasource.Temporary

class DeviceTypeMapper {

    fun toStringValue(type: DeviceType) : String =
        when (type) {
            Permanent -> "permanent"
            Temporary -> "temporary"
            LegalHold -> "legalhold"
            else -> "unknown"
        }
}
