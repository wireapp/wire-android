package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.Conversation

class ConversationListAdapter(
    private val viewHolderInflater: ViewHolderInflater,
    diffCallback: ConversationListDiffCallback
) : PagedListAdapter<Conversation, ConversationViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder =
        ConversationViewHolder(parent, viewHolderInflater)

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) } //TODO what does null mean?
    }
}

class ConversationListDiffCallback : DiffUtil.ItemCallback<Conversation>() {

    override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean =
        oldItem.name == newItem.name //TODO check everything that concerns UI
}
