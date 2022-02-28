package com.wire.android.ui.home.conversations

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.Message

data class ConversationViewState(
    val conversationName: String = "",
    val messages: List<Message> = emptyList(),
    val messageText : TextFieldValue = TextFieldValue("")
)
