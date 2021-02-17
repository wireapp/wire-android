package com.wire.android.feature.conversation.list.ui.icon

import coil.loadAny
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.setCircularShape
import com.wire.android.core.extension.setCorneredShape

interface ConversationIcon {
    fun displayOn(imageView: ShapeableImageView)
}

class NoParticipantsConversationIcon : ConversationIcon {
    override fun displayOn(imageView: ShapeableImageView) {
        imageView.setCorneredShape(R.dimen.conversation_list_toolbar_team_icon_corner_radius)
    }
}

class SingleParticipantConversationIcon(private val contactIcon: ContactIcon<*>) : ConversationIcon {

    override fun displayOn(imageView: ShapeableImageView) {
        with(imageView) {
            setCircularShape()
            loadAny(contactIcon.create(context, width, height))
        }
    }
}

class GroupConversationIcon(private val contactIcons: List<ContactIcon<*>>) : ConversationIcon {

    override fun displayOn(imageView: ShapeableImageView) {
        with(imageView) {
            setCorneredShape(R.dimen.conversation_list_toolbar_team_icon_corner_radius)
            loadAny(contactIcons[0].create(context, width, height))
        }
    }
}
