package com.wire.android.ui.main.conversationlist.model

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.ui.main.conversationlist.common.UserInfoLabel


data class ConversationFolder(
    val folderName: String,
)

sealed class Conversation {
    data class GroupConversation(
        val groupColorValue: Long,
        val groupName: String
    ) : Conversation()

    data class PrivateConversation(
        val userInfo: UserInfo,
        val conversationInfo: ConversationInfo
    ) : Conversation()
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

fun Conversation.PrivateConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = conversationInfo.isLegalHold,
        membership = conversationInfo.membership,
    )


