package com.wire.android.feature.conversation.list.ui.icon

import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ui.icon.ContactIconLoader

class SingleParticipantConversationIcon(private val contact: Contact, private val contactIconLoader: ContactIconLoader) : ConversationIcon {

    override fun displayOn(imageView: ShapeableImageView) {
        contactIconLoader
            .load(contact) { circleCrop() }
            .into(imageView)
    }
}
