package com.wire.android.feature.conversation.list.ui.icon

import android.content.Context
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import coil.load
import coil.loadAny
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.setCorneredShape
import com.wire.android.feature.contact.ui.icon.ContactIcon

class GroupConversationIcon(private val contactIcons: List<ContactIcon>) : ConversationIcon {

    override fun displayOn(imageView: ShapeableImageView) {
        with(imageView) {
            setCorneredShape(R.dimen.conversation_list_toolbar_team_icon_corner_radius)

            val layerDrawable = ContextCompat.getDrawable(context, R.drawable.group_conversation_icon) as LayerDrawable
            clearLayerDrawable(context, layerDrawable)
            load(layerDrawable)

            contactIcons.forEachIndexed { index, icon ->
                val data = icon.create(context, width / GRID_SIZE, height / GRID_SIZE)
                loadAny(data) {
                    target {
                        layerDrawable.setDrawable(index, it)
                        layerDrawable.invalidateSelf()
                    }
                }
            }

        }
    }

    private fun clearLayerDrawable(context: Context, layerDrawable: LayerDrawable) =
        (0 until MAX_ICON_COUNT).forEach {
            layerDrawable.setDrawable(it, ContextCompat.getDrawable(context, R.drawable.empty_contact_icon))
        }

    companion object {
        const val MAX_ICON_COUNT = 4
        private const val GRID_SIZE = 2
    }
}
