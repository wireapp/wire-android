package com.wire.android.ui.conversation.all.model

import com.wire.android.R


data class ConversationFolder(
    val folderName: String,
)

data class Conversation(
    val userInfo: UserInfo,
    val conversationInfo: ConversationInfo
)

data class ConversationInfo(
    val name: String,
    val memberShip: Membership = Membership.None,
    val isLegalHold: Boolean = false
)

data class UserInfo(
    val avatarUrl: String = "",
    val availabilityStatus: AvailabilityStatus = AvailabilityStatus.None
)

enum class AvailabilityStatus {
    Available, Busy, Away, None
}

enum class Membership(val stringResourceId: Int) {
    Guest(R.string.label_membership_guest), External(R.string.label_memebership_external), None(-1)
}


