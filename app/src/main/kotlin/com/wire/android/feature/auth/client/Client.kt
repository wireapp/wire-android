package com.wire.android.feature.auth.client

import com.wire.android.core.config.DeviceClass
import com.wire.android.core.config.DeviceType
import com.wire.android.core.config.Permanent
import com.wire.android.core.config.Phone
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.extension.EMPTY

data class Client(
    val id: String,
    val deviceType: DeviceType = Permanent,
    val label: String? = String.EMPTY,
    val password: String = String.EMPTY,
    val model: String = String.EMPTY,
    val deviceClass: DeviceClass = Phone,
    val preKeys: List<PreKey> = listOf(),
    val lastKey: PreKey = PreKey(-1, String.EMPTY),
    val refreshToken: String = String.EMPTY,
    val registrationTime: String = String.EMPTY
)
