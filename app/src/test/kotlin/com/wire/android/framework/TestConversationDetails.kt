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
        lastMessage = null,
        unreadEventCount = emptyMap()
    )

    val GROUP = ConversationDetails.Group(
        TestConversation.ONE_ON_ONE,
        LegalHoldStatus.DISABLED,
        lastMessage = null,
        isSelfUserCreator = true,
        isSelfUserMember = true,
        unreadEventCount = emptyMap(),
        selfRole = Conversation.Member.Role.Member
    )

}
