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

class ConversationViewHolder(
    parent: ViewGroup, inflater: ViewHolderInflater
) : RecyclerView.ViewHolder(inflater.inflate(R.layout.conversation_list_item, parent)) {

    private val nameTextView by lazyFind<TextView>(R.id.conversationItemNameTextView)

    private val iconImageView by lazyFind<ShapeableImageView>(R.id.conversationItemIconImageView)

    fun bind(item: ConversationListItem) {
        val name = item.name ?: item.id
        nameTextView.text = name

        val nameOfFirstMember = item.members.getOrNull(0)?.name
        val nameInitial = nameOfFirstMember?.getOrNull(0).toStringOrEmpty()
        iconImageView.afterMeasured {
            it.load(TextDrawable(text = nameInitial, width = it.width.toFloat(), height = it.height.toFloat()))
        }
    }
}
