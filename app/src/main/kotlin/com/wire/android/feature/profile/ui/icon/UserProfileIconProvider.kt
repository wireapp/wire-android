package com.wire.android.feature.profile.ui.icon

import com.wire.android.shared.asset.ui.imageloader.IconLoader
import com.wire.android.shared.user.User

class UserProfileIconProvider(private val iconLoader: IconLoader) {

    fun provide(user: User) = UserProfileIcon(user, iconLoader)
}
