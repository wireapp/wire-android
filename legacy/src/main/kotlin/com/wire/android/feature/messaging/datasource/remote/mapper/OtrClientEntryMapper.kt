package com.wire.android.feature.messaging.datasource.remote.mapper

import com.google.protobuf.ByteString
import com.wire.android.feature.messaging.ClientPayload
import com.wire.messages.Otr

class OtrClientEntryMapper(private val clientIdMapper: OtrClientIdMapper) {

    fun toOtrClientEntry(clientPayload: ClientPayload): Otr.ClientEntry = Otr.ClientEntry.newBuilder()
        .setClient(clientIdMapper.toOtrClientId(clientPayload.clientId))
        .setText(ByteString.copyFrom(clientPayload.payload))
        .build()
}
