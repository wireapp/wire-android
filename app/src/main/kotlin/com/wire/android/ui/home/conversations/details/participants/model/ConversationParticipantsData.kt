package com.wire.android.ui.home.conversations.details.participants.model

data class ConversationParticipantsData(
    val admins: List<UIParticipant> = listOf(),
    val participants: List<UIParticipant> = listOf(),
    val allAdminsCount: Int = 0,
    val allParticipantsCount: Int = 0,
    val isSelfAnAdmin: Boolean = false
) {
    val allCount: Int = allAdminsCount + allParticipantsCount
}
