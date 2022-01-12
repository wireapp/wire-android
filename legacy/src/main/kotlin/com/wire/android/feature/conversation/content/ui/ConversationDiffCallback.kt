package com.wire.android.feature.conversation.content.ui

import androidx.recyclerview.widget.DiffUtil

class ConversationDiffCallback : DiffUtil.ItemCallback<CombinedMessageContact>() {

    override fun areItemsTheSame(oldItem: CombinedMessageContact, newItem: CombinedMessageContact) =
        oldItem.message.id == newItem.message.id && oldItem.message.time == newItem.message.time

    override fun areContentsTheSame(oldItem: CombinedMessageContact, newItem: CombinedMessageContact): Boolean =
        oldItem.message.content == newItem.message.content
}
