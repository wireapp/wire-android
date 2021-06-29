package com.wire.android.feature.auth.client.mapper

import com.wire.android.core.crypto.model.PreKey
import com.wire.android.feature.auth.client.datasource.remote.api.PreKeyRequest

class PreKeyMapper {
    fun toPreKeyRequest(preKey: PreKey): PreKeyRequest {
        return PreKeyRequest(preKey.id, preKey.encodedData)
    }
}
