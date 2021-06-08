package com.wire.android.feature.messaging.datasource.remote.mapper

import com.google.protobuf.ByteString
import com.wire.android.feature.messaging.ChatMessageEnvelope
import com.wire.messages.Otr

class OtrMessageMapper(
    private val clientIdMapper: OtrClientIdMapper,
    private val userIdMapper: OtrUserIdMapper
) {

    fun fromMessageEnvelope(envelope: ChatMessageEnvelope): Otr.NewOtrMessage {
        val recipientData = envelope.recipients.map { recipientEntry ->
            val clientEntries = recipientEntry.clientPayloads.map { clientPayload ->
                Otr.ClientEntry.newBuilder()
                    .setClient(clientIdMapper.toOtrClientId(clientPayload.clientId))
                    .setText(ByteString.copyFrom(clientPayload.payload))
                    .build()
            }
            Otr.UserEntry.newBuilder()
                .addAllClients(clientEntries)
                .setUser(userIdMapper.toOtrUserId(recipientEntry.userId))
                .build()
        }

        val builder = Otr.NewOtrMessage.newBuilder()
            .addAllRecipients(recipientData)
            .setSender(clientIdMapper.toOtrClientId(envelope.senderClientId))

        //TODO Add other fields
        if (envelope.dataBlob != null) builder.blob = ByteString.copyFrom(envelope.dataBlob)

        return builder.build()
    }

}
