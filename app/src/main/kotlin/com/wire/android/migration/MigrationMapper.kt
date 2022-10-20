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
            type = Conversation.Type.valueOf(type.toString()),
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
}
