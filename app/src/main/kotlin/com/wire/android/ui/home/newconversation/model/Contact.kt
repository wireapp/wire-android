package com.wire.android.ui.home.newconversation.model

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserStatus
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

data class Contact(
    val id: String,
    val domain: String,
    val name: String,
    val userStatus: UserStatus = UserStatus.NONE,
    val avatarAsset: UserAvatarAsset? = null,
    val label: String = "",
    val connectionState: ConnectionState = ConnectionState.NOT_CONNECTED
) {

    fun toMember(): Member {
        return Member(UserId(id, domain))
    }

    val isConnectedOrPending =
        connectionState == ConnectionState.ACCEPTED ||
            connectionState == ConnectionState.SENT ||
            connectionState == ConnectionState.PENDING
}

fun OtherUser.toContact() =
    Contact(
        id = id.value,
        domain = id.domain,
        name = name ?: "",
        label = handle ?: "",
        avatarAsset = completePicture?.let { UserAvatarAsset(it) },
        connectionState = connectionStatus
    )
