package com.wire.android.shared.user.mapper

import com.wire.android.feature.auth.registration.datasource.remote.RegisteredUserResponse
import com.wire.android.shared.user.User
import com.wire.android.shared.user.datasources.local.UserEntity
import com.wire.android.shared.user.datasources.remote.SelfUserResponse

class UserMapper {

    fun fromSelfUserResponse(response: SelfUserResponse) =
        User(id = response.id, name = response.name, email = response.email)

    fun fromRegisteredUserResponse(response: RegisteredUserResponse) =
        User(id = response.id, name = response.name, email = response.email)

    fun toUserEntity(user: User) = UserEntity(id = user.id, name = user.name, email = user.email)
}
