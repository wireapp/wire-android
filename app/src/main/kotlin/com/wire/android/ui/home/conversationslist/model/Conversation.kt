package com.wire.android.ui.home.conversationslist.model

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.main.conversationlist.common.UserInfoLabel


interface Conversation {
    val conversationType: ConversationType
}

sealed class ConversationType {
    data class GroupConversation(
        val groupColorValue: Long,
        val groupName: String
    ) : ConversationType()

    data class PrivateConversation(
        val userInfo: UserInfo,
        val conversationInfo: ConversationInfo
    ) : ConversationType()
}

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

fun ConversationType.PrivateConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = conversationInfo.isLegalHold,
        membership = conversationInfo.membership,
    )
