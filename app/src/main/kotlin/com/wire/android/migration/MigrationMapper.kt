package com.wire.android.migration

import com.wire.android.migration.globalDatabase.ScalaSsoIdEntity
import com.wire.android.migration.userDatabase.ScalaConversationData
import com.wire.android.util.orDefault
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.PlainId
import com.wire.kalium.logic.data.user.SsoId
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationMapper @Inject constructor() {
    fun fromScalaSsoID(ssoIdEntity: ScalaSsoIdEntity): SsoId = with(ssoIdEntity) {
        SsoId(
            subject = subject,
            tenant = tenant,
            scimExternalId = null
        )
    }

    fun fromScalaConversationToConversation(scalaConversation: ScalaConversationData) = with(scalaConversation) {
        Conversation(
            id = ConversationId(remoteId, domain.orDefault("wire.com")),
            name = name,
            type = mapConversationType(type),
            teamId = null, // can we get this from user?
            protocol = Conversation.ProtocolInfo.Proteus,
            mutedStatus = MutedConversationStatus.AllAllowed,
            creatorId = PlainId(creatorId),
            access = listOf(),
            accessRole = listOf(),
            removedBy = null,
            lastReadDate = LocalDateTime.MIN.toString(),
            lastModifiedDate = LocalDateTime.MIN.toString(),
            lastNotificationDate = LocalDateTime.MIN.toString()
        )
    }

    // UNKNOWN(-1), GROUP(0), SELF(1), ONE_TO_ONE(2), WAIT_FOR_CONNECTION(3), INCOMING_CONNECTION(4)
    private fun mapConversationType(type: Int): Conversation.Type = when (type) {
        0 -> Conversation.Type.GROUP
        1 -> Conversation.Type.SELF
        2 -> Conversation.Type.ONE_ON_ONE
        3, 4 -> Conversation.Type.CONNECTION_PENDING
        else -> Conversation.Type.ONE_ON_ONE // what should we do here ?
    }
}
