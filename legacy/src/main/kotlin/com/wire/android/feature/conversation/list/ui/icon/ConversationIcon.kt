package com.wire.android.feature.conversation.list.ui.icon

import android.content.Context
import android.graphics.drawable.Drawable
import com.google.android.material.imageview.ShapeableImageView

interface ConversationIcon {
    fun background(context: Context): Drawable? = null

    fun displayOn(imageView: ShapeableImageView)
}
