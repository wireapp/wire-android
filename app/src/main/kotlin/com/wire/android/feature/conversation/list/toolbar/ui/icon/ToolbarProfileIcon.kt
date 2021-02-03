package com.wire.android.feature.conversation.list.toolbar.ui.icon

import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.shared.team.Team
import com.wire.android.shared.user.User

abstract class ToolbarProfileIcon {
    abstract fun displayOn(imageView: ShapeableImageView)

    companion object {
        fun forUser(user: User): ToolbarProfileIcon = PersonalUserToolbarProfileIcon() //TODO: pass user's profile picture

        fun forTeam(team: Team): ToolbarProfileIcon = TeamUserToolbarProfileIcon(team.name, team.icon)
    }
}
