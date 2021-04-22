package com.wire.android.feature.profile.ui.icon

import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.shared.asset.ui.imageloader.AvatarLoader
import com.wire.android.shared.user.User

class UserProfileIcon(private val user: User, private val avatarLoader: AvatarLoader) {
    fun displayOn(imageView: ShapeableImageView) {
        avatarLoader
            .load(user.profilePicture, user.name, imageView)
            .into(imageView)
    }
}
