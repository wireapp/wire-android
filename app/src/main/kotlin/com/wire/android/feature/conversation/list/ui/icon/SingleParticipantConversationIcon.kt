package com.wire.android.feature.conversation.list.ui.icon

import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.feature.contact.Contact
import com.wire.android.shared.asset.ui.imageloader.AvatarLoader

class SingleParticipantConversationIcon(private val contact: Contact, private val avatarLoader: AvatarLoader) : ConversationIcon {

    override fun displayOn(imageView: ShapeableImageView) {
        avatarLoader
            .load(contact.profilePicture, contact.name, imageView) { circleCrop() }
            .into(imageView)
    }
}
