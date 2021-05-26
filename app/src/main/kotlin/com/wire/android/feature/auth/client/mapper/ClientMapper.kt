package com.wire.android.feature.auth.client.mapper

import com.wire.android.core.config.DeviceConfig
import com.wire.android.core.config.Permanent
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.SignalingKeyRequest
import com.wire.android.shared.config.DeviceClassMapper
import com.wire.android.shared.config.DeviceTypeMapper

class ClientMapper(
    private val deviceTypeMapper: DeviceTypeMapper,
    private val deviceClassMapper: DeviceClassMapper,
    private val deviceConfig: DeviceConfig,
    private val preKeyMapper: PreKeyMapper
) {
    fun toClientRegistrationRequest(client: Client) = ClientRegistrationRequest(
        client.id,
        preKeyMapper.toPreKeyRequest(client.lastKey),
        client.preKeys.map(preKeyMapper::toPreKeyRequest),
        SignalingKeyRequest(),
        deviceTypeMapper.value(client.deviceType),
        deviceClassMapper.value(client.deviceClass),
        client.model,
        client.password,
        client.label
    )

    fun newClient(userId: String, password: String, preKeyInitialization: PreKeyInitialization) =
        Client(
            userId,
            Permanent,
            deviceConfig.deviceName(),
            password,
            deviceConfig.deviceModelName(),
            deviceConfig.deviceClass(),
            preKeyInitialization.createdKeys,
            preKeyInitialization.lastKey
        )
}
