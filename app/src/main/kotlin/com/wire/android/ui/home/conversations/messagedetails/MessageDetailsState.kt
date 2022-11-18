package com.wire.android.ui.home.conversations.messagedetails

import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReactionsData

data class MessageDetailsState(
    val isSelfMessage: Boolean = false, // Default is false as Read Receipts is yet not implemented and we don't need to know it for now.
    val reactionsData: MessageDetailsReactionsData = MessageDetailsReactionsData()
)
