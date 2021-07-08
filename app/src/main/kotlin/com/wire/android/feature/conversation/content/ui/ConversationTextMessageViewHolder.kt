package com.wire.android.feature.conversation.content.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.lazyFind
import com.wire.android.core.ui.recyclerview.ViewHolderInflater

class ConversationTextMessageViewHolder(parent: ViewGroup, inflater: ViewHolderInflater) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.conversation_chat_item_text, parent)) {

    private val conversationChatItemUsernameTextView by lazyFind<TextView>(R.id.conversationChatItemUsernameTextView)
    private val conversationChatItemTextMessageTextView by lazyFind<TextView>(R.id.conversationChatItemTextMessageTextView)
    private val conversationChatItemUserAvatarImageView by lazyFind<ShapeableImageView>(R.id.conversationChatItemUserAvatarImageView)

    fun bind(message: MessageAndContact, shouldShowAvatar: Boolean) {
        if (shouldShowAvatar)
            conversationChatItemUserAvatarImageView.visibility = View.VISIBLE
        else
            conversationChatItemUserAvatarImageView.visibility = View.GONE

        conversationChatItemUsernameTextView.text = message.contact.name
        conversationChatItemTextMessageTextView.text = message.message.content
    }
}
