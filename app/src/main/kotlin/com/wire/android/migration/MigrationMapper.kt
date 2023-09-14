/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.migration

import androidx.annotation.VisibleForTesting
import com.wire.android.migration.globalDatabase.ScalaSsoIdEntity
import com.wire.android.migration.userDatabase.ScalaConversationData
import com.wire.android.migration.userDatabase.ScalaMessageData
import com.wire.android.migration.userDatabase.ScalaUserData
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.Access
import com.wire.kalium.logic.data.conversation.Conversation.ProtocolInfo
import com.wire.kalium.logic.data.conversation.Conversation.Type
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.message.MigratedMessage
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.SsoId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.util.DateTimeUtil
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("MagicNumber")
@Singleton
class MigrationMapper @Inject constructor() {
    fun fromScalaSsoID(ssoIdEntity: ScalaSsoIdEntity): SsoId = with(ssoIdEntity) {
        SsoId(
            subject = subject,
            tenant = tenant,
            scimExternalId = null
        )
    }

    @VisibleForTesting
    fun toQualifiedId(remoteId: String, domain: String?, selfUserId: UserId): QualifiedID {
        val actualDomain = if (domain.isNullOrEmpty()) {
            selfUserId.domain
        } else {
            domain
        }

        return QualifiedID(
            value = remoteId,
            domain = actualDomain
        )
    }

    fun fromScalaConversationToConversation(scalaConversation: ScalaConversationData, selfUserId: UserId) = with(scalaConversation) {
        mapConversationType(type)?.let {
            val lastEventTime: String =
                if (orderTime == null || orderTime == 0L) {
                    "1970-01-01T00:00:00.000Z"
                } else {
                    DateTimeUtil.fromEpochMillisToIsoDateTimeString(orderTime)
                }

            val conversationLastReadTime = if (lastReadTime == null || lastReadTime == 0L) {
                "1970-01-01T00:00:00.000Z"
            } else {
                DateTimeUtil.fromEpochMillisToIsoDateTimeString(lastReadTime)
            }

            Conversation(
                id = toQualifiedId(remoteId, domain, selfUserId),
                name = name,
                type = it,
                teamId = scalaConversation.teamId?.let { teamId -> TeamId(teamId) },
                protocol = ProtocolInfo.Proteus(Conversation.VerificationStatus.NOT_VERIFIED),
                mutedStatus = mapMutedStatus(mutedStatus),
                access = mapAccess(access),
                accessRole = emptyList(),
                removedBy = null,
                lastReadDate = conversationLastReadTime,
                lastModifiedDate = lastEventTime,
                lastNotificationDate = lastEventTime,
                creatorId = creatorId,
                receiptMode = fromScalaReceiptMode(receiptMode),
                messageTimer = null,
                userMessageTimer = null,
                archived = false,
                archivedDateTime = null
            )
        }
    }

    private fun fromScalaReceiptMode(receiptMode: Int?): Conversation.ReceiptMode = receiptMode?.let {
        if (receiptMode > 0) {
            Conversation.ReceiptMode.ENABLED
        } else {
            Conversation.ReceiptMode.DISABLED
        }
    } ?: Conversation.ReceiptMode.DISABLED

    fun fromScalaMessageToMessage(selfUserId: UserId, scalaMessage: ScalaMessageData, scalaSenderUserData: ScalaUserData) =
        with(scalaMessage) {
            MigratedMessage(
                conversationId = toQualifiedId(conversationRemoteId, conversationDomain, selfUserId),
                senderUserId = toQualifiedId(scalaSenderUserData.id, scalaSenderUserData.domain, selfUserId),
                senderClientId = ClientId(senderClientId.orEmpty()),
                timestamp = time,
                content = content.orEmpty(),
                encryptedProto = proto,
                assetName = assetName,
                assetSize = assetSize,
                editTime = editTime,
                unencryptedProto = null
            )
        }

    private fun mapAccess(access: String): List<Access> {
        val accessList = access.removeSurrounding("[", "]").replace("\"", "").split(",").map { it.trim() }
        return accessList.map {
            when (access) {
                "private" -> Access.PRIVATE
                "invite" -> Access.INVITE
                "link" -> Access.LINK
                "code" -> Access.CODE
                else -> Access.PRIVATE
            }
        }
    }

    private fun mapMutedStatus(status: Int): MutedConversationStatus = when (status) {
        0 -> MutedConversationStatus.AllAllowed
        1 -> MutedConversationStatus.OnlyMentionsAndRepliesAllowed
        2 -> MutedConversationStatus.OnlyMentionsAndRepliesAllowed
        3 -> MutedConversationStatus.AllMuted
        else -> MutedConversationStatus.AllAllowed
    }

    private fun mapConversationType(type: Int): Type? = when (type) {
        0 -> Type.GROUP
        1 -> Type.SELF
        2 -> Type.ONE_ON_ONE
        3, 4 -> Type.CONNECTION_PENDING
        else -> null
    }

    private fun mapUserAvailabilityStatus(status: Int): UserAvailabilityStatus = when (status) {
        1 -> UserAvailabilityStatus.AVAILABLE
        2 -> UserAvailabilityStatus.AWAY
        3 -> UserAvailabilityStatus.BUSY
        else -> UserAvailabilityStatus.NONE
    }

    private fun mapConnectionStatus(connectionStatus: String): ConnectionState = when (connectionStatus) {
        "self" -> ConnectionState.ACCEPTED
        "sent" -> ConnectionState.SENT
        "pending" -> ConnectionState.PENDING
        "accepted" -> ConnectionState.ACCEPTED
        "blocked" -> ConnectionState.BLOCKED
        "ignored" -> ConnectionState.IGNORED
        "cancelled" -> ConnectionState.CANCELLED
        "missing-legalhold-consent" -> ConnectionState.MISSING_LEGALHOLD_CONSENT
        "unconnected" -> ConnectionState.NOT_CONNECTED
        else -> ConnectionState.NOT_CONNECTED
    }

    @Suppress("ComplexMethod")
    fun fromScalaUserToUser(
        scalaUserData: ScalaUserData,
        selfUserId: String,
        selfUserDomain: String?,
        selfUserTeamId: String?,
        selfuser: UserId
    ) =
        if (scalaUserData.id == selfUserId && scalaUserData.domain == selfUserDomain) {
            SelfUser(
                id = toQualifiedId(scalaUserData.id, scalaUserData.domain, selfuser),
                name = scalaUserData.name,
                handle = scalaUserData.handle,
                email = scalaUserData.email,
                phone = scalaUserData.phone,
                accentId = scalaUserData.accentId,
                teamId = scalaUserData.teamId?.let { TeamId(it) },
                connectionStatus = ConnectionState.ACCEPTED,
                previewPicture = scalaUserData.pictureAssetId?.let { toQualifiedId(it, scalaUserData.domain, selfuser) },
                completePicture = scalaUserData.pictureAssetId?.let { toQualifiedId(it, scalaUserData.domain, selfuser) },
                availabilityStatus = mapUserAvailabilityStatus(scalaUserData.availability)
            )
        } else {
            val botService =
                if (scalaUserData.serviceIntegrationId == null || scalaUserData.serviceProviderId == null) null
                else BotService(scalaUserData.serviceIntegrationId, scalaUserData.serviceProviderId)
            val userType = when {
                botService != null -> UserType.SERVICE
                scalaUserData.domain != selfUserDomain -> UserType.FEDERATED
                scalaUserData.teamId != null && scalaUserData.teamId == selfUserTeamId -> UserType.INTERNAL
                selfUserTeamId != null -> UserType.GUEST
                else -> UserType.NONE
            }
            OtherUser(
                id = toQualifiedId(scalaUserData.id, scalaUserData.domain, selfuser),
                name = scalaUserData.name,
                handle = scalaUserData.handle,
                email = scalaUserData.email,
                phone = scalaUserData.phone,
                accentId = scalaUserData.accentId,
                teamId = scalaUserData.teamId?.let { TeamId(it) },
                connectionStatus = mapConnectionStatus(scalaUserData.connection),
                previewPicture = scalaUserData.pictureAssetId?.let { toQualifiedId(it, scalaUserData.domain, selfuser) },
                completePicture = scalaUserData.pictureAssetId?.let { toQualifiedId(it, scalaUserData.domain, selfuser) },
                userType = userType,
                availabilityStatus = mapUserAvailabilityStatus(scalaUserData.availability),
                botService = botService,
                deleted = scalaUserData.deleted,
                defederated = false
            )
        }
}
