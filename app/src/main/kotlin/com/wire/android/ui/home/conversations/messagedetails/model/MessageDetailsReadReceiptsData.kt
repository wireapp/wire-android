package com.wire.android.ui.home.conversations.messagedetails.model

import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant

data class MessageDetailsReadReceiptsData(
    val readReceipts: List<UIParticipant> = listOf()
)
