package com.wire.android.framework

import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.ProtocolInfo
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.user.type.UserType

object TestConversationDetails {

    val CONNECTION = ConversationDetails.Connection(
        TestConversation.ID,
        TestUser.OTHER_USER,
        UserType.EXTERNAL,
        "2022-03-30T15:36:00.000Z",
        TestConnection.CONNECTION,
        protocolInfo = ProtocolInfo.Proteus,
        access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
        accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST)
    )

    val CONVERSATION_ONE_ONE = ConversationDetails.OneOne(
        TestConversation.ONE_ON_ONE,
        TestUser.OTHER_USER,
        LegalHoldStatus.DISABLED,
        UserType.EXTERNAL,
        unreadRepliesCount = 0,
        lastMessage = null,
        unreadEventCount = emptyMap()
    )

    val GROUP = ConversationDetails.Group(
        TestConversation.ONE_ON_ONE,
        LegalHoldStatus.DISABLED,
        unreadRepliesCount = 0,
        lastMessage = null,
        isSelfUserCreator = true,
        isSelfUserMember = true,
        unreadEventCount = emptyMap()
    )

}
