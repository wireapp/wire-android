package com.wire.android.feature.conversation.content.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.wire.android.R
import com.wire.android.core.extension.lazyFind
import com.wire.android.core.extension.timeFromOffsetDateTime
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.conversation.content.Content
import com.wire.android.shared.asset.ui.imageloader.UserAvatarProvider
import com.wire.android.shared.conversation.content.ConversationTimeGenerator
import kotlinx.android.synthetic.main.conversation_chat_time_separator.view.*

class ConversationTextMessageViewHolder(
    parent: ViewGroup,
    inflater: ViewHolderInflater,
    private val userAvatarProvider: UserAvatarProvider,
    private val conversationTimeGenerator: ConversationTimeGenerator
) : RecyclerView.ViewHolder(inflater.inflate(R.layout.conversation_chat_item_text, parent)) {

    private val conversationChatItemUsernameTextView by lazyFind<TextView>(R.id.conversationChatItemUsernameTextView)
    private val conversationChatItemTextMessageTextView by lazyFind<TextView>(R.id.conversationChatItemTextMessageTextView)
    private val conversationChatItemUserAvatarImageView by lazyFind<ShapeableImageView>(R.id.conversationChatItemUserAvatarImageView)
    private val conversationChatItemTimeTextView by lazyFind<TextView>(R.id.conversationChatItemTimeTextView)
    private val conversationNotSameDaySeparatorView by lazyFind<View>(R.id.conversationNotSameDaySeparatorView)
    private val conversationSameDaySeparatorView by lazyFind<View>(R.id.conversationSameDaySeparatorView)
    private val conversationTimeSeparatorTextTextView by lazyFind<View>(R.id.conversationTimeSeparatorTextTextView)
    private val timeSeparator by lazyFind<View>(R.id.timeSeparator)

    fun bindMessage(
        combinedMessage: CombinedMessageContact,
        showUserAvatar: Boolean,
        showNewDaySeparator: Boolean,
        showSameDaySeparator: Boolean
    ) {
        setUpAvatar(showUserAvatar, combinedMessage.contact)
        setUpTimeSeparatorVisibility(showNewDaySeparator, showSameDaySeparator)
        initItemClick()
        val message = combinedMessage.message
        conversationTimeSeparatorTextTextView.conversationTimeSeparatorTextTextView.text =
            conversationTimeGenerator.separatorTimeLabel(message.time)
        conversationChatItemUsernameTextView.text = combinedMessage.contact.name
        conversationChatItemTextMessageTextView.text = (message.content as Content.Text).value //TODO Handle multiple content types
        conversationChatItemTimeTextView.text = message.time.timeFromOffsetDateTime()
    }

    private fun setUpAvatar(shouldShowAvatar: Boolean, contact: Contact) {
        if (shouldShowAvatar) {
            conversationChatItemUserAvatarImageView.visibility = View.VISIBLE
            userAvatarProvider.provide(contact.profilePicture, contact.name)?.displayOn(conversationChatItemUserAvatarImageView)
        } else conversationChatItemUserAvatarImageView.visibility = View.GONE
    }

    private fun setUpTimeSeparatorVisibility(showNewDaySeparator: Boolean, showSameDaySeparator: Boolean) {
        timeSeparator.isVisible = showNewDaySeparator || showSameDaySeparator
        when {
            showNewDaySeparator -> {
                conversationNotSameDaySeparatorView.visibility = View.VISIBLE
                conversationSameDaySeparatorView.visibility = View.GONE
            }
            showSameDaySeparator -> {
                conversationSameDaySeparatorView.visibility = View.VISIBLE
                conversationNotSameDaySeparatorView.visibility = View.GONE
            }
            else -> {
                timeSeparator.visibility = View.GONE
                conversationSameDaySeparatorView.visibility = View.GONE
                conversationNotSameDaySeparatorView.visibility = View.GONE
            }
        }
    }

    private fun initItemClick() {
        conversationChatItemTextMessageTextView.setOnClickListener {
            conversationChatItemTimeTextView.isVisible = !conversationChatItemTimeTextView.isVisible
        }
    }
}
