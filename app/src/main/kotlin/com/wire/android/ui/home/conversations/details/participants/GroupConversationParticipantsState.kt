package com.wire.android.ui.home.conversations.details.participants

import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.kalium.logic.data.user.UserId

data class GroupConversationParticipantsState(
    val data: ConversationParticipantsData = ConversationParticipantsData()
) {
    val showAllVisible: Boolean get() = data.allParticipantsCount > data.participants.size || data.allAdminsCount > data.admins.size

    companion object {
        val PREVIEW = GroupConversationParticipantsState(
            data = ConversationParticipantsData(
                admins = listOf(UIParticipant(UserId("0", ""), "name", "handle", false)),
                participants = listOf(UIParticipant(UserId("1", ""), "name", "handle", false)),
                allAdminsCount = 1,
                allParticipantsCount = 1
            )
        )
    }
}
