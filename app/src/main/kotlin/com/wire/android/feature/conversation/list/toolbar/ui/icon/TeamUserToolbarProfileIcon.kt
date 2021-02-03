package com.wire.android.feature.conversation.list.toolbar.ui.icon

import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.setCorneredShape
import com.wire.android.core.ui.drawable.TextDrawable
import com.wire.android.shared.asset.Asset

internal class TeamUserToolbarProfileIcon(private val teamName: String, private val icon: Asset?) : ToolbarProfileIcon() {
    override fun displayOn(imageView: ShapeableImageView) {
        with(imageView) {
            setCorneredShape(R.dimen.conversation_list_toolbar_team_icon_corner_radius)

            if (icon == null) {
                val teamNameDrawable = TextDrawable(
                    text = teamName.firstOrNull().toString(),
                    width = width.toFloat(), height = height.toFloat()
                )
                load(teamNameDrawable)
            } else {
                //TODO: load team icon
            }
        }
    }
}
