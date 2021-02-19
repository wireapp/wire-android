package com.wire.android.feature.conversation.list.ui.icon

import coil.loadAny
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.core.extension.setCircularShape
import com.wire.android.feature.contact.ui.icon.ContactIcon

class SingleParticipantConversationIcon(private val contactIcon: ContactIcon) : ConversationIcon {

    override fun displayOn(imageView: ShapeableImageView) {
        with(imageView) {
            setCircularShape()
            loadAny(contactIcon.create(context, width, height))
        }
    }
}
