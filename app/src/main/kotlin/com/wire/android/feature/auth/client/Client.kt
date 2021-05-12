package com.wire.android.feature.auth.client

import com.wire.android.core.config.DeviceClass
import com.wire.android.core.config.DeviceType
import com.wire.android.core.crypto.model.PreKey

data class Client(
    val id: String,
    val deviceType: DeviceType,
    val label: String?,
    val password: String,
    val model: String,
    val deviceClass: DeviceClass,
    val preKeys: List<PreKey>,
    val lastKey: PreKey
)
