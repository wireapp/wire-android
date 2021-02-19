package com.wire.android.feature.conversation.list.ui.icon

import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.setCorneredShape

class NoParticipantsConversationIcon : ConversationIcon {
    override fun displayOn(imageView: ShapeableImageView) {
        with(imageView) {
            setCorneredShape(R.dimen.conversation_list_toolbar_team_icon_corner_radius)
            load(R.drawable.empty_contact_icon)
        }
    }
}
