package com.wire.android.feature.auth.client.mapper

import com.wire.android.core.config.DeviceConfig
import com.wire.android.core.config.Permanent
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.feature.auth.client.datasource.local.ClientEntity
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.datasource.remote.api.SignalingKeyRequest
import com.wire.android.shared.config.DeviceClassMapper
import com.wire.android.shared.config.DeviceTypeMapper

class ClientMapper(
    private val deviceTypeMapper: DeviceTypeMapper,
    private val deviceClassMapper: DeviceClassMapper,
    private val deviceConfig: DeviceConfig,
    private val preKeyMapper: PreKeyMapper
) {

    fun fromClientResponseToClientEntity(clientResponse: ClientResponse) =
        ClientEntity(clientResponse.id)

    fun newRegistrationRequest(userId: String, password: String, preKeyInitialization: PreKeyInitialization): ClientRegistrationRequest {

        val lastPreKey = with(preKeyInitialization.lastKey) { preKeyMapper.toPreKeyRequest(id, encodedData) }
        val preKeys = preKeyInitialization.createdKeys.map { preKeyMapper.toPreKeyRequest(it.id, it.encodedData) }
        return ClientRegistrationRequest(
            userId,
            lastPreKey,
            preKeys,
            SignalingKeyRequest(),
            deviceTypeMapper.value(Permanent),
            deviceClassMapper.value(deviceConfig.deviceClass()),
            deviceConfig.deviceModelName(),
            password,
            deviceConfig.deviceName()
        )
    }
}
