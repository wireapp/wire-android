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

package com.wire.android.ui.home.conversations.details.participants

import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId

data class GroupConversationParticipantsState(
    val data: ConversationParticipantsData = ConversationParticipantsData()
) {
    val showAllVisible: Boolean get() = data.allParticipantsCount > data.participants.size || data.allAdminsCount > data.admins.size
    val addParticipantsEnabled: Boolean get() = data.isSelfAnAdmin && !data.isSelfExternalMember

    companion object {
        val PREVIEW = GroupConversationParticipantsState(
            data = ConversationParticipantsData(
                admins = listOf(
                    UIParticipant(
                        id = UserId("0", ""),
                        name = "admin",
                        handle = "handle",
                        isSelf = true,
                        supportedProtocolList = listOf(SupportedProtocol.MLS)
                    )
                ),
                participants = listOf(
                    UIParticipant(
                        id = UserId("1", ""),
                        name = "participant",
                        handle = "handle",
                        isSelf = true,
                        supportedProtocolList = listOf(SupportedProtocol.PROTEUS)
                    )
                ),
                allAdminsCount = 1,
                allParticipantsCount = 1,
                isSelfAnAdmin = true
            )
        )
    }
}
