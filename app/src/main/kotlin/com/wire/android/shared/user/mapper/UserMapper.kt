package com.wire.android.shared.user.mapper

import com.wire.android.feature.auth.registration.datasource.remote.RegisteredUserResponse
import com.wire.android.shared.asset.PublicAsset
import com.wire.android.shared.asset.datasources.remote.AssetResponse
import com.wire.android.shared.asset.mapper.AssetMapper
import com.wire.android.shared.user.User
import com.wire.android.shared.user.datasources.local.UserEntity
import com.wire.android.shared.user.datasources.remote.SelfUserResponse

class UserMapper(private val assetMapper: AssetMapper) {

    fun fromSelfUserResponse(response: SelfUserResponse): User {
        return User(
            id = response.id,
            name = response.name,
            email = response.email,
            username = response.handle,
            profilePicture = generateProfilePicture(response.assets)
        )
    }

    private fun generateProfilePicture(assets: List<AssetResponse>) =
        assetMapper.profilePictureAssetKey(assets)?.let { PublicAsset(it) }

    fun fromRegisteredUserResponse(response: RegisteredUserResponse) =
        User(id = response.id, name = response.name, email = response.email, username = response.handle)

    fun fromUserEntity(entity: UserEntity) =
        User(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            username = entity.username,
            profilePicture = entity.assetKey?.let { PublicAsset(it) })

    fun toUserEntity(user: User) = UserEntity(
        id = user.id,
        name = user.name,
        email = user.email,
        username = user.username,
        assetKey = (user.profilePicture as? PublicAsset)?.key
    )
}
