package com.wire.android.feature.conversation.list.ui.icon

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.feature.contact.Contact
import com.wire.android.shared.asset.ui.imageloader.IconLoader

class GroupConversationIcon(private val contacts: List<Contact>, private val iconLoader: IconLoader) : ConversationIcon {

    override fun background(context: Context): Drawable? =
        ContextCompat.getDrawable(context, R.drawable.conversation_icon_border)

    override fun displayOn(imageView: ShapeableImageView) = with(imageView) {

        val layerDrawable = ContextCompat.getDrawable(context, R.drawable.group_conversation_icon) as LayerDrawable
        clearLayerDrawable(context, layerDrawable)
        imageView.setImageDrawable(layerDrawable)

        contacts.forEachIndexed { index, contact ->
            iconLoader.load(contact.profilePicture, contact.name, imageView)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        layerDrawable.setDrawable(index, resource)
                        layerDrawable.invalidateSelf()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        layerDrawable.setDrawable(index, placeholder ?: emptyIcon(context))
                        layerDrawable.invalidateSelf()
                    }
                })
        }
    }

    private fun clearLayerDrawable(context: Context, layerDrawable: LayerDrawable) =
        (0 until MAX_ICON_COUNT).forEach {
            layerDrawable.setDrawable(it, emptyIcon(context))
        }

    private fun emptyIcon(context: Context) = ContextCompat.getDrawable(context, R.drawable.empty_contact_icon)

    companion object {
        const val MAX_ICON_COUNT = 4
    }
}
