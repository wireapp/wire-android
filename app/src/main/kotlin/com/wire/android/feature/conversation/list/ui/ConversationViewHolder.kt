package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.clear
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.config.LocaleConfig
import com.wire.android.core.extension.afterMeasured
import com.wire.android.core.extension.lazyFind
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.drawable.TextDrawable
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import java.io.File

class ConversationViewHolder(
    parent: ViewGroup, inflater: ViewHolderInflater,
    private val localeConfig: LocaleConfig
) : RecyclerView.ViewHolder(inflater.inflate(R.layout.conversation_list_item, parent)) {

    private val nameTextView by lazyFind<TextView>(R.id.conversationItemNameTextView)

    private val iconImageView by lazyFind<ShapeableImageView>(R.id.conversationItemIconImageView)

    fun bind(item: ConversationListItem) {
        val name = item.name.orEmpty() //TODO: handle empty name case properly
        nameTextView.text = name

        displayConversationIcon(item)
    }

    private fun displayConversationIcon(item: ConversationListItem) {
        iconImageView.clear()

        //TODO: display up to 4 images
        item.members.firstOrNull()?.let { member ->
            if (member.profilePicturePath != null) {
                iconImageView.load(File(member.profilePicturePath))
            } else {
                val nameInitial = member.name.firstOrNull().toStringOrEmpty().toUpperCase(localeConfig.currentLocale())
                iconImageView.afterMeasured {
                    it.load(TextDrawable(text = nameInitial, width = it.width.toFloat(), height = it.height.toFloat()))
                }
            }
        }
    }
}
