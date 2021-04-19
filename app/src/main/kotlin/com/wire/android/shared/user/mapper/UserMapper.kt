package com.wire.android.shared.user.mapper

import com.wire.android.feature.auth.registration.datasource.remote.RegisteredUserResponse
import com.wire.android.shared.asset.PublicAsset
import com.wire.android.shared.user.User
import com.wire.android.shared.user.datasources.local.UserEntity
import com.wire.android.shared.user.datasources.remote.SelfUserResponse

class UserMapper {

    fun fromSelfUserResponse(response: SelfUserResponse) =
        User(id = response.id, name = response.name, email = response.email, username = response.handle)

    fun fromRegisteredUserResponse(response: RegisteredUserResponse) =
        User(id = response.id, name = response.name, email = response.email, username = response.handle)

    fun fromUserEntity(entity: UserEntity) =
        User(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            username = entity.username,
            assetKey = entity.assetKey,
            profilePicture = entity.assetKey?.let { PublicAsset(it) })

    fun toUserEntity(user: User) = UserEntity(
        id = user.id,
        name = user.name,
        email = user.email,
        username = user.username,
        assetKey = user.assetKey
    )
}
