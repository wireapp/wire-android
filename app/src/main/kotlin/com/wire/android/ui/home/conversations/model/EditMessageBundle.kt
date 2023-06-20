package com.wire.android.ui.home.conversations.model

import com.wire.android.ui.home.messagecomposer.UiMention

data class EditMessageBundle(
    val originalMessageId: String,
    val newContent: String,
    val newMentions: List<UiMention>,
)
