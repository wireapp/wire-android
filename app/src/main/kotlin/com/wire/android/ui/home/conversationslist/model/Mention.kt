package com.wire.android.ui.home.conversationslist.model

data class Mention(
    val mentionInfo: MentionInfo,
    val conversationType: ConversationType
)

data class MentionInfo(val mentionMessage: MentionMessage)

data class MentionMessage(val message: String) {
    fun toQuote(): String = "\"$message\""
}
