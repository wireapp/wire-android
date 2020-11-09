package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.list.usecase.Conversation

class ConversationListAdapter(
    private val viewHolderInflater: ViewHolderInflater = ViewHolderInflater()
) : RecyclerView.Adapter<ConversationViewHolder>() {

    private var items: List<Conversation> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder =
        ConversationViewHolder(parent, viewHolderInflater)

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) =
        holder.bind(items[position])

    fun updateData(items: List<Conversation>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size
}
