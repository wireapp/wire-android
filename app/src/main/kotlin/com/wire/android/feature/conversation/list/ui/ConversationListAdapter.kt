package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.list.ui.icon.ConversationIconProvider

class ConversationListAdapter(
    private val viewHolderInflater: ViewHolderInflater,
    diffCallback: ConversationListDiffCallback,
    private val iconProvider: ConversationIconProvider
) : PagingDataAdapter<ConversationListItem, ConversationViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder =
        ConversationViewHolder(parent, viewHolderInflater, iconProvider)

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) } //TODO what does null mean?
    }
}

class ConversationListDiffCallback : DiffUtil.ItemCallback<ConversationListItem>() {

    override fun areItemsTheSame(oldItem: ConversationListItem, newItem: ConversationListItem): Boolean =
        oldItem.conversation.id == newItem.conversation.id

    override fun areContentsTheSame(oldItem: ConversationListItem, newItem: ConversationListItem): Boolean =
        oldItem.conversation == newItem.conversation && areMembersTheSame(oldItem, newItem)

    private fun areMembersTheSame(oldItem: ConversationListItem, newItem: ConversationListItem): Boolean =
        oldItem.members.size == newItem.members.size && oldItem.members.containsAll(newItem.members)
}
