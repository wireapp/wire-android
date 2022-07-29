package com.wire.android.ui.userprofile.common

import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.type.UserType

object UsernameMapper {

    fun mapUserLabel(otherUser: OtherUser): String = with(otherUser) {
        val userId = otherUser.id
        return when (otherUser.userType) {
            UserType.FEDERATED -> if (handle != null) "$handle@${userId.domain}" else ""
            else -> handle.orEmpty()
        }
    }
}
