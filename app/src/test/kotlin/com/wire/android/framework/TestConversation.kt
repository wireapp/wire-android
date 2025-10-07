/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.framework

import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.Conversation.ProtocolInfo
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.mls.CipherSuite
import com.wire.kalium.logic.data.user.UserId
import kotlin.time.Instant

object TestConversation {
    val ID = ConversationId("valueConvo", "domainConvo")

    fun id(suffix: Int = 0) = ConversationId("valueConvo_$suffix", "domainConvo")

    val ONE_ON_ONE = Conversation(
        ID.copy(value = "1O1 ID"),
        "ONE_ON_ONE Name",
        Conversation.Type.OneOnOne,
        TestTeam.TEAM_ID,
        ProtocolInfo.Proteus,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        lastReadDate = Instant.parse("2022-03-30T15:36:00.000Z"),
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        creatorId = null,
        receiptMode = Conversation.ReceiptMode.ENABLED,
        messageTimer = null,
        userMessageTimer = null,
        archived = false,
        archivedDateTime = null,
        mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        legalHoldStatus = Conversation.LegalHoldStatus.DISABLED
    )
    val SELF = Conversation(
        ID.copy(value = "SELF ID"),
        "SELF Name",
        Conversation.Type.Self,
        TestTeam.TEAM_ID,
        ProtocolInfo.Proteus,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        lastReadDate = Instant.parse("2022-03-30T15:36:00.000Z"),
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        creatorId = null,
        receiptMode = Conversation.ReceiptMode.ENABLED,
        messageTimer = null,
        userMessageTimer = null,
        archived = false,
        archivedDateTime = null,
        mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        legalHoldStatus = Conversation.LegalHoldStatus.DISABLED
    )

    fun GROUP(protocolInfo: ProtocolInfo = ProtocolInfo.Proteus) = Conversation(
        ID,
        "GROUP Name",
        Conversation.Type.Group.Regular,
        TestTeam.TEAM_ID,
        protocolInfo,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        lastReadDate = Instant.parse("2022-03-30T15:36:00.000Z"),
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        creatorId = null,
        receiptMode = Conversation.ReceiptMode.ENABLED,
        messageTimer = null,
        userMessageTimer = null,
        archived = false,
        archivedDateTime = null,
        mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        legalHoldStatus = Conversation.LegalHoldStatus.DISABLED
    )

    fun one_on_one(convId: ConversationId) = Conversation(
        convId,
        "ONE_ON_ONE Name",
        Conversation.Type.OneOnOne,
        TestTeam.TEAM_ID,
        ProtocolInfo.Proteus,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        lastReadDate = Instant.parse("2022-03-30T15:36:00.000Z"),
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        creatorId = null,
        receiptMode = Conversation.ReceiptMode.ENABLED,
        messageTimer = null,
        userMessageTimer = null,
        archived = false,
        archivedDateTime = null,
        mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        legalHoldStatus = Conversation.LegalHoldStatus.DISABLED
    )

    val USER_1 = UserId("member1", "domainMember")
    val MEMBER_TEST1 = Member(USER_1, Member.Role.Admin)
    val USER_2 = UserId("member2", "domainMember")
    val MEMBER_TEST2 = Member(USER_2, Member.Role.Member)

    val GROUP_ID = GroupID("mlsGroupId")

    val CONVERSATION = Conversation(
        ConversationId("conv_id", "domain"),
        "ONE_ON_ONE Name",
        Conversation.Type.OneOnOne,
        TestTeam.TEAM_ID,
        ProtocolInfo.Proteus,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        lastReadDate = Instant.parse("2022-03-30T15:36:00.000Z"),
        creatorId = null,
        receiptMode = Conversation.ReceiptMode.ENABLED,
        messageTimer = null,
        userMessageTimer = null,
        archived = false,
        archivedDateTime = null,
        mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        legalHoldStatus = Conversation.LegalHoldStatus.DISABLED
    )

    val MLS_PROTOCOL_INFO = ProtocolInfo.MLS(
        GROUP_ID,
        ProtocolInfo.MLSCapable.GroupState.PENDING_JOIN,
        0UL,
        Instant.parse("2021-03-30T15:36:00.000Z"),
        cipherSuite = CipherSuite.MLS_128_DHKEMX25519_AES128GCM_SHA256_Ed25519
    )
}
