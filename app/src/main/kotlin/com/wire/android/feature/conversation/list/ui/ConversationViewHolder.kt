package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wire.android.R
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.Conversation

//TODO: implement proper layout
class ConversationViewHolder(parent: ViewGroup, inflater: ViewHolderInflater) : RecyclerView.ViewHolder(
    inflater.inflate(R.layout.item_conversation_list, parent)
) {
    fun bind(conversation: Conversation) {
        (itemView as TextView).text = conversation.name ?: conversation.id
    }
}
