package com.wire.android.ui.main.conversation.model

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.main.conversation.common.components.UserInfoLabel

data class ConversationFolder(
    val folderName: String,
)

data class Conversation(
    val userInfo: UserInfo,
    val conversationInfo: ConversationInfo
)

fun Conversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = conversationInfo.isLegalHold,
        membership = conversationInfo.membership,
    )

data class ConversationInfo(
    val name: String,
    val membership: Membership = Membership.None,
    val isLegalHold: Boolean = false
)

data class UserInfo(
    val avatarUrl: String = "",
    val availabilityStatus: AvailabilityStatus = AvailabilityStatus.None
)

enum class AvailabilityStatus {
    Available, Busy, Away, None
}

enum class Membership(@StringRes val stringResourceId: Int) {
    Guest(R.string.label_membership_guest), External(R.string.label_memebership_external), None(-1)
}


