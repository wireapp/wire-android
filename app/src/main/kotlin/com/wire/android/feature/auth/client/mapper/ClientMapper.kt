package com.wire.android.feature.auth.client.mapper

import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.PreKeyRequest
import com.wire.android.feature.auth.client.datasource.remote.api.SignalingKeyRequest
import com.wire.android.shared.config.DeviceClassMapper
import com.wire.android.shared.config.DeviceTypeMapper

class ClientMapper(private val deviceTypeMapper: DeviceTypeMapper, private val deviceClassMapper: DeviceClassMapper) {

    fun toClientRegistrationRequest(client: Client) = ClientRegistrationRequest(
            client.id,
            PreKeyRequest(client.lastKey.id, client.lastKey.encodedData),
            client.preKeys.map { PreKeyRequest(it.id, it.encodedData) },
            SignalingKeyRequest(),
            deviceTypeMapper.toStringValue(client.deviceType),
            deviceClassMapper.toStringValue(client.deviceClass),
            client.model,
            client.password,
            client.label
        )
}
