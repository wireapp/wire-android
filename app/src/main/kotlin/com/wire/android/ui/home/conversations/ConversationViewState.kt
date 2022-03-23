package com.wire.android.ui.home.conversations

import android.os.Parcelable
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.Message
import kotlinx.parcelize.Parcelize

data class ConversationViewState(
    val conversationName: String = "",
    val messages: List<Message> = emptyList(),
    val messageText: TextFieldValue = TextFieldValue("")
)


