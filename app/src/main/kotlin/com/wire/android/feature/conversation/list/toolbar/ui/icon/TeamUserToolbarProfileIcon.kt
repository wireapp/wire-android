package com.wire.android.feature.conversation.list.toolbar.ui.icon

import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.load
import com.wire.android.core.extension.setCorneredShape
import com.wire.android.core.ui.drawable.TextDrawable
import com.wire.android.shared.asset.Asset

internal class TeamUserToolbarProfileIcon(private val teamName: String, private val icon: Asset?) : ToolbarProfileIcon {

    override fun displayOn(imageView: ShapeableImageView) {
        imageView.setCorneredShape(R.dimen.conversation_list_toolbar_team_icon_corner_radius)

        if (icon == null) displayTeamNameAsIcon(imageView)
        else displayTeamIcon(imageView)
    }

    private fun displayTeamNameAsIcon(imageView: ShapeableImageView) = with(imageView) {
        val teamNameInitial = teamName.firstOrNull().toString()
        imageView.load(TextDrawable(teamNameInitial))
    }

    private fun displayTeamIcon(imageView: ShapeableImageView) {
        //TODO: load team icon
    }
}
