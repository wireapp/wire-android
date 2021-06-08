package com.wire.android.feature.messaging.datasource.remote.mapper

import com.wire.messages.Otr
import java.math.BigInteger

class OtrClientIdMapper {

    fun toOtrClientId(clientId: String): Otr.ClientId {
        val longId = BigInteger(clientId, CLIENT_ID_RADIX).toLong()
        return Otr.ClientId.newBuilder()
            .setClient(longId)
            .build()
    }

    fun fromOtrClientId(otrClientId: Otr.ClientId): String = otrClientId.client.toBigInteger().toString(CLIENT_ID_RADIX)

    companion object {
        private const val CLIENT_ID_RADIX = 16
    }
}
