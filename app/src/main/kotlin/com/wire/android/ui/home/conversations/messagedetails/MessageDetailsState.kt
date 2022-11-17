package com.wire.android.ui.home.conversations.messagedetails

import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReactionsData

data class MessageDetailsState(
    val isSelfMessage: Boolean = false,
    val reactionsData: MessageDetailsReactionsData = MessageDetailsReactionsData()
)
