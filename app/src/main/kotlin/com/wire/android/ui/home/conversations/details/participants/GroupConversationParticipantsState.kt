package com.wire.android.ui.home.conversations.details.participants

import com.wire.android.ui.home.conversations.model.UIParticipant

data class GroupConversationParticipantsState(
    val admins: List<UIParticipant> = listOf(),
    val participants: List<UIParticipant> = listOf(),
    val allAdminsCount: Int = 0,
    val allParticipantsCount: Int = 0
)
