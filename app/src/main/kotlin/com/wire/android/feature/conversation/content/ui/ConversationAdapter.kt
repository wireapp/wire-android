package com.wire.android.feature.conversation.content.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wire.android.core.extension.isLastSixtyMinutes
import com.wire.android.core.extension.isSameDay
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.shared.asset.ui.imageloader.UserAvatarProvider
import com.wire.android.shared.conversation.content.TimeGenerator

class ConversationAdapter(
    private val viewHolderInflater: ViewHolderInflater,
    private val userAvatarProvider: UserAvatarProvider,
    private val timeGenerator: TimeGenerator
) : ListAdapter<CombinedMessageContact, RecyclerView.ViewHolder>(ConversationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ConversationTextMessageViewHolder(parent, viewHolderInflater, userAvatarProvider, timeGenerator)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_TEXT_MESSAGE) {
            val currentHolder = holder as ConversationTextMessageViewHolder
            val combinedMessageContact = (getItem(position) as CombinedMessageContact)
            var showUserAvatar = true
            var showNewDaySeparator = true
            var showSameDaySeparator = false
            if(position > 0) {
                val currentMessage = combinedMessageContact.message
                val previousMessage = (getItem(position - 1) as CombinedMessageContact).message
                showUserAvatar = currentMessage.senderUserId != previousMessage.senderUserId
                showNewDaySeparator = !currentMessage.time.isSameDay(previousMessage.time)
                showSameDaySeparator = previousMessage.time.isLastSixtyMinutes(currentMessage.time)
            }

            currentHolder.bindMessage(combinedMessageContact, showUserAvatar, showNewDaySeparator, showSameDaySeparator)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CombinedMessageContact -> VIEW_TYPE_TEXT_MESSAGE
            else -> VIEW_TYPE_UNKNOWN
        }
    }

    companion object {
        const val VIEW_TYPE_TEXT_MESSAGE = 10
        const val VIEW_TYPE_UNKNOWN = -1
    }
}
