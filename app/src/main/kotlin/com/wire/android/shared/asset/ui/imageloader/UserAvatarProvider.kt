package com.wire.android.shared.asset.ui.imageloader

import com.wire.android.shared.asset.Asset

class UserAvatarProvider(private val avatarLoader: AvatarLoader) {

    fun provide(profilePicture: Asset?, name: String) =
        profilePicture?.let { UserAvatar(it, name, avatarLoader) }
}
