package com.wire.android.feature.profile.ui.icon

import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.shared.asset.ui.imageloader.IconLoader
import com.wire.android.shared.user.User

class UserProfileIcon(private val user: User, private val iconLoader: IconLoader) {
    fun displayOn(imageView: ShapeableImageView) {
        iconLoader
            .load(
                user.profilePicture,
                user.name,
                imageView,
                withCirclePlaceholder = true
            ) { circleCrop() }
            .into(imageView)
    }
}
