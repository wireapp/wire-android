package com.wire.android.ui.userprofile.common

import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.type.UserType

object UsernameMapper {
    fun OtherUser.toUserLabel(): String {
        return when (userType) {
            UserType.FEDERATED -> if (handle != null) "$handle@${id.domain}" else ""
            else -> handle.orEmpty()
        }
    }

}

