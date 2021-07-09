package com.wire.android.feature.conversation.content.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wire.android.core.ui.recyclerview.ViewHolderInflater

class ConversationAdapter(private val viewHolderInflater: ViewHolderInflater) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messages: List<Any> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ConversationTextMessageViewHolder(parent, viewHolderInflater)


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_TEXT_MESSAGE) {
            val shouldShowAvatar = shouldShowAvatar(position)
            (holder as ConversationTextMessageViewHolder).bind(
                (messages[position] as MessageAndContact),
                shouldShowAvatar
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is MessageAndContact -> VIEW_TYPE_TEXT_MESSAGE
            else -> VIEW_TYPE_UNKNOWN
        }
    }

    override fun getItemCount(): Int = messages.size

    fun setList(newItems: List<Any>) {
        this.messages = newItems
        notifyDataSetChanged()
    }

    private fun shouldShowAvatar(position: Int): Boolean {
        val currentMessage = (messages[position] as MessageAndContact).message
        return (position == 0) ||
                (position > 0 && currentMessage.senderUserId != (messages[position - 1] as MessageAndContact).message.senderUserId)
    }

    companion object {
        const val VIEW_TYPE_TEXT_MESSAGE = 10
        const val VIEW_TYPE_UNKNOWN = -1
    }
}
