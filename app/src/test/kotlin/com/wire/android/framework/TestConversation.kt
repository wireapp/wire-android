package com.wire.android.framework

import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.Conversation.ProtocolInfo
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.id.PlainId
import com.wire.kalium.logic.data.user.UserId

object TestConversation {
    val ID = ConversationId("valueConvo", "domainConvo")

    fun id(suffix: Int = 0) = ConversationId("valueConvo_$suffix", "domainConvo")

    val ONE_ON_ONE = Conversation(
        ID.copy(value = "1O1 ID"),
        "ONE_ON_ONE Name",
        Conversation.Type.ONE_ON_ONE,
        TestTeam.TEAM_ID,
        ProtocolInfo.Proteus,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        lastReadDate = "2022-03-30T15:36:00.000Z",
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        isCreator = false,
        isSelfUserMember = true
    )
    val SELF = Conversation(
        ID.copy(value = "SELF ID"),
        "SELF Name",
        Conversation.Type.SELF,
        TestTeam.TEAM_ID,
        ProtocolInfo.Proteus,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        lastReadDate = "2022-03-30T15:36:00.000Z",
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        isCreator = false,
        isSelfUserMember = true
    )

    fun GROUP(protocolInfo: ProtocolInfo = ProtocolInfo.Proteus) = Conversation(
        ID,
        "GROUP Name",
        Conversation.Type.GROUP,
        TestTeam.TEAM_ID,
        protocolInfo,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        lastReadDate = "2022-03-30T15:36:00.000Z",
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        isCreator = true,
        isSelfUserMember = true
    )


    fun one_on_one(convId: ConversationId) = Conversation(
        convId,
        "ONE_ON_ONE Name",
        Conversation.Type.ONE_ON_ONE,
        TestTeam.TEAM_ID,
        ProtocolInfo.Proteus,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        lastReadDate = "2022-03-30T15:36:00.000Z",
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        isCreator = false,
        isSelfUserMember = true
    )

    val USER_1 = UserId("member1", "domainMember")
    val MEMBER_TEST1 = Member(USER_1, Member.Role.Admin)
    val USER_2 = UserId("member2", "domainMember")
    val MEMBER_TEST2 = Member(USER_2, Member.Role.Member)

    val GROUP_ID = GroupID("mlsGroupId")

    val CONVERSATION = Conversation(
        ConversationId("conv_id", "domain"),
        "ONE_ON_ONE Name",
        Conversation.Type.ONE_ON_ONE,
        TestTeam.TEAM_ID,
        ProtocolInfo.Proteus,
        MutedConversationStatus.AllAllowed,
        null,
        null,
        null,
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
        lastReadDate = "2022-03-30T15:36:00.000Z",
        isCreator = false,
        isSelfUserMember = true
    )

}
