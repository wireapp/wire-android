package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.clear
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.lazyFind
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.list.ui.icon.ConversationIconProvider

class ConversationViewHolder(
    parent: ViewGroup, inflater: ViewHolderInflater,
    private val iconProvider: ConversationIconProvider
) : RecyclerView.ViewHolder(inflater.inflate(R.layout.conversation_list_item, parent)) {

    private val nameTextView by lazyFind<TextView>(R.id.conversationItemNameTextView)

    private val iconLayout by lazyFind<FrameLayout>(R.id.conversationItemIconLayout)

    private val iconImageView by lazyFind<ShapeableImageView>(R.id.conversationItemIconImageView)

    fun bind(item: ConversationListItem) {
        val name = item.conversation.name.orEmpty() //TODO: handle empty name case properly
        nameTextView.text = name

        displayConversationIcon(item)
    }

    private fun displayConversationIcon(item: ConversationListItem) {
        iconImageView.clear()

        iconProvider.provide(item).let {
            iconLayout.background = it.background(iconLayout.context)
            it.displayOn(iconImageView)
        }
    }
}
