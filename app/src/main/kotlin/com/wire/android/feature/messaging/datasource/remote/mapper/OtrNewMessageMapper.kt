package com.wire.android.feature.messaging.datasource.remote.mapper

import com.google.protobuf.ByteString
import com.wire.android.feature.messaging.ChatMessageEnvelope
import com.wire.messages.Otr

class OtrNewMessageMapper(
    private val clientIdMapper: OtrClientIdMapper,
    private val userEntryMapper: OtrUserEntryMapper
) {

    fun fromMessageEnvelope(envelope: ChatMessageEnvelope): Otr.NewOtrMessage {
        val recipientData = envelope.recipients.map(userEntryMapper::toOtrUserEntry)

        val builder = Otr.NewOtrMessage.newBuilder()
            .addAllRecipients(recipientData)
            .setSender(clientIdMapper.toOtrClientId(envelope.senderClientId))

        //TODO Add other fields
        if (envelope.dataBlob != null) builder.blob = ByteString.copyFrom(envelope.dataBlob)

        return builder.build()
    }

}
