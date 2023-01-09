package com.wire.android.ui.home.conversations.messagedetails.model

import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant

data class MessageDetailsReactionsData(
    val reactions: Map<String, List<UIParticipant>> = mapOf()
)
