package com.wire.android.feature.profile.ui.icon

import com.wire.android.shared.asset.ui.imageloader.AvatarLoader
import com.wire.android.shared.user.User

class UserProfileIconProvider(private val avatarLoader: AvatarLoader) {

    fun provide(user: User) = UserProfileIcon(user, avatarLoader)
}
