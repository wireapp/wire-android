package com.wire.android.feature.messaging.datasource.remote.mapper

import com.wire.android.feature.messaging.RecipientEntry
import com.wire.messages.Otr

class OtrUserEntryMapper(
    private val userIdMapper: OtrUserIdMapper,
    private val clientEntryMapper: OtrClientEntryMapper
) {

    fun toOtrUserEntry(recipientEntry: RecipientEntry): Otr.UserEntry = Otr.UserEntry.newBuilder()
        .addAllClients(recipientEntry.clientPayloads.map(clientEntryMapper::toOtrClientEntry))
        .setUser(userIdMapper.toOtrUserId(recipientEntry.userId))
        .build()
}
