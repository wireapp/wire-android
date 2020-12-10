package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.wire.android.core.ui.recyclerview.ViewHolderInflater

class ConversationListAdapter(
    private val viewHolderInflater: ViewHolderInflater,
    diffCallback: ConversationListDiffCallback
) : PagedListAdapter<ConversationListItem, ConversationViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder =
        ConversationViewHolder(parent, viewHolderInflater)

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) } //TODO what does null mean?
    }
}

class ConversationListDiffCallback : DiffUtil.ItemCallback<ConversationListItem>() {

    override fun areItemsTheSame(oldItem: ConversationListItem, newItem: ConversationListItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ConversationListItem, newItem: ConversationListItem): Boolean =
        oldItem.name == newItem.name && areMembersTheSame(oldItem, newItem)

    private fun areMembersTheSame(oldItem: ConversationListItem, newItem: ConversationListItem): Boolean =
        oldItem.members.size == newItem.members.size && oldItem.members.containsAll(newItem.members)
}
