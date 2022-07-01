package com.wire.android.ui.home.newconversation.model

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

data class Contact(
    val id: String,
    val domain: String,
    val name: String,
    val avatarData: UserAvatarData = UserAvatarData(),
    val label: String = "",
    val connectionState: ConnectionState,
    val membership: Membership
) {

    fun toMember(): Member {
        return Member(UserId(id, domain))
    }
}
