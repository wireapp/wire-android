package com.wire.android.feature.conversation.list.ui.icon

import coil.loadAny
import coil.transform.CircleCropTransformation
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.feature.contact.ui.icon.ContactIcon

class SingleParticipantConversationIcon(private val contactIcon: ContactIcon) : ConversationIcon {
    override fun displayOn(imageView: ShapeableImageView) {
        with(imageView) {
            loadAny(contactIcon.create(context, width, height)) {
                transformations(CircleCropTransformation())
            }
        }
    }
}
