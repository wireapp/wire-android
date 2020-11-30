package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.afterMeasured
import com.wire.android.core.extension.lazyFind
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.drawable.TextDrawable
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.Conversation

class ConversationViewHolder(
    parent: ViewGroup, inflater: ViewHolderInflater
) : RecyclerView.ViewHolder(inflater.inflate(R.layout.conversation_list_item, parent)) {

    private val nameTextView by lazyFind<TextView>(R.id.conversationItemNameTextView)

    private val iconImageView by lazyFind<ShapeableImageView>(R.id.conversationItemIconImageView)

    fun bind(conversation: Conversation) {
        val name = conversation.name ?: conversation.id
        nameTextView.text = name

        //TODO: show member name initial
        //TODO: how can it be null??
        iconImageView.afterMeasured {
            it.load(TextDrawable(text = name.firstOrNull().toStringOrEmpty(), width = it.width.toFloat(), height = it.height.toFloat()))
        }
    }
}
