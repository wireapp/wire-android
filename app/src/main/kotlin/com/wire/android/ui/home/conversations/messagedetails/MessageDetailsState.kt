package com.wire.android.ui.home.conversations.messagedetails

import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReactionsData
import com.wire.android.ui.home.conversations.messagedetails.model.MessageDetailsReadReceiptsData

data class MessageDetailsState(
    val isSelfMessage: Boolean = false,
    val reactionsData: MessageDetailsReactionsData = MessageDetailsReactionsData(),
    val readReceiptsData: MessageDetailsReadReceiptsData = MessageDetailsReadReceiptsData()
)
