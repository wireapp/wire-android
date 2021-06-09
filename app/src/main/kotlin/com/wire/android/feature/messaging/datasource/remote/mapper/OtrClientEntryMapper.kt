package com.wire.android.feature.messaging.datasource.remote.mapper

import com.google.protobuf.ByteString
import com.wire.android.feature.messaging.ClientPayload
import com.wire.messages.Otr

class OtrClientEntryMapper(private val clientIdMapper: OtrClientIdMapper) {

    fun toOtrClientEntry(clientPayload: ClientPayload): Otr.ClientEntry {
        val builder = Otr.ClientEntry.newBuilder()
        builder.setClient(clientIdMapper.toOtrClientId(clientPayload.clientId))
        builder.setText(ByteString.copyFrom(clientPayload.payload))
        return builder.build()
    }

}