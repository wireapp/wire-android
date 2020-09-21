package com.wire.android.shared.user.mapper

import com.wire.android.shared.user.User
import com.wire.android.shared.user.datasources.remote.SelfUserResponse

class UserMapper {

    fun fromSelfUserResponse(response: SelfUserResponse) =
        User(id = response.id, name = response.name, email = response.email)
}
