package com.wire.android.ui.conversation.mention.model

import com.wire.android.ui.conversation.all.model.Conversation

data class Mention(
    val mentionInfo: MentionInfo,
    val conversation: Conversation
)

data class MentionInfo(val mentionMessage: MentionMessage)

data class MentionMessage(val message: String) {
    fun toQuote() = "\"$message"\""
}
