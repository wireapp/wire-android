package com.wire.android.feature.conversation.list.toolbar.ui.icon

import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.core.extension.setCircularShape

internal class PersonalUserToolbarProfileIcon : ToolbarProfileIcon {

    override fun displayOn(imageView: ShapeableImageView) {
        imageView.setCircularShape()
        //TODO: display user profile picture
    }
}
