package com.wire.android.feature.auth.client.mapper

import com.wire.android.core.config.DeviceConfig
import com.wire.android.core.config.Permanent
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.datasource.local.ClientEntity
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.datasource.remote.api.PreKeyRequest
import com.wire.android.feature.auth.client.datasource.remote.api.SignalingKeyRequest
import com.wire.android.shared.config.DeviceClassMapper
import com.wire.android.shared.config.DeviceTypeMapper

class ClientMapper(
    private val deviceTypeMapper: DeviceTypeMapper,
    private val deviceClassMapper: DeviceClassMapper,
    private val deviceConfig: DeviceConfig
) {

    fun fromClientResponseToClientEntity(clientResponse: ClientResponse) = with(clientResponse){
        ClientEntity(id, refreshToken, registrationTime)
    }

    fun toClientRegistrationRequest(client: Client) = ClientRegistrationRequest(
        client.id,
        PreKeyRequest(client.lastKey.id, client.lastKey.encodedData),
        client.preKeys.map { PreKeyRequest(it.id, it.encodedData) },
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
