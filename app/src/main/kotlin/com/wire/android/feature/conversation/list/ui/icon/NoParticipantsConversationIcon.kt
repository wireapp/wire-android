package com.wire.android.feature.conversation.list.ui.icon

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.load

class NoParticipantsConversationIcon : ConversationIcon {

    override fun background(context: Context): Drawable? =
        ContextCompat.getDrawable(context, R.drawable.conversation_icon_border)

    override fun displayOn(imageView: ShapeableImageView) {
        imageView.load(R.drawable.empty_contact_icon)
    }
}
