package com.wire.android.shared.asset.ui.imageloader

import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.shared.asset.Asset

class UserAvatar(private val profilePicture: Asset, private val name: String, private val avatarLoader: AvatarLoader) {
    fun displayOn(imageView: ShapeableImageView) {
        avatarLoader
            .load(profilePicture, name, imageView)
            .into(imageView)
    }
}
