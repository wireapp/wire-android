package com.wire.android.feature.conversation.list.ui.icon

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ui.icon.ContactIconLoader

class GroupConversationIcon(private val contacts: List<Contact>, private val contactIconLoader: ContactIconLoader) : ConversationIcon {

    override fun background(context: Context): Drawable? =
        ContextCompat.getDrawable(context, R.drawable.conversation_icon_border)

    override fun displayOn(imageView: ShapeableImageView) = with(imageView) {

        val layerDrawable = ContextCompat.getDrawable(context, R.drawable.group_conversation_icon) as LayerDrawable
        clearLayerDrawable(context, layerDrawable)
        load(layerDrawable)

        contacts.forEachIndexed { index, contact ->
            contactIconLoader.load(contact, imageView = this) {
                target {
                    layerDrawable.setDrawable(index, it)
                    layerDrawable.invalidateSelf()
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
    }
}
