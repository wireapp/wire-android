package com.wire.android.feature.auth.client.mapper

import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.PreKeyRequest
import com.wire.android.feature.auth.client.datasource.remote.api.SignalingKeyRequest

class ClientMapper {

    fun toClientRegistrationRequest(client: Client) =
        ClientRegistrationRequest(
            client.id,
            PreKeyRequest(client.lastKey.id, client.lastKey.encodedData),
            client.preKeys.map { PreKeyRequest(it.id, it.encodedData) },
            SignalingKeyRequest(
                client.signalingKey.encryptionKey,
                client.signalingKey.macKey
            ),
            client.type,
            client.deviceType,
            client.model,
            client.password,
            client.label
        )
}
