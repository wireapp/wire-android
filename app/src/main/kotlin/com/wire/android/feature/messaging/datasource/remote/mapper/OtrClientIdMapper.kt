package com.wire.android.feature.messaging.datasource.remote.mapper

import com.wire.messages.Otr
import java.math.BigInteger

class OtrClientIdMapper {

    fun toOtrClientId(clientId: String): Otr.ClientId = Otr.ClientId.newBuilder()
        .setClient(BigInteger(clientId, CLIENT_ID_RADIX).toLong())
        .build()

    fun fromOtrClientId(otrClientId: Otr.ClientId): String = otrClientId.client.toBigInteger().toString(CLIENT_ID_RADIX)

    companion object {
        private const val CLIENT_ID_RADIX = 16
    }
}
