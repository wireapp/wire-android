package com.wire.android.shared.prekey.data.remote

import com.wire.android.core.crypto.model.PreKey

class RemotePreKeyMapper {

    fun fromRemoteResponse(preKeyResponse: PreKeyResponse): PreKey = PreKey(preKeyResponse.id, preKeyResponse.key)

}
