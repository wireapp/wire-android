package com.wire.android.feature.auth.client.mapper

import com.wire.android.feature.auth.client.datasource.local.ClientEntity
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse

class ClientMapper {
    fun fromClientResponseToEntity(clientResponse: ClientResponse) = ClientEntity(clientResponse.id)
}
