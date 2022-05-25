package com.wire.android.ui.home.conversationslist.model

import com.wire.android.R
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId

data class ConnectionInfo(val connectionState: ConnectionState, val userId: UserId)

// We can expand this to show more detailed messages for other cases
fun ConnectionState.toMessageId(): Int = when (this) {
    ConnectionState.PENDING -> R.string.connection_pending_message
    else -> -1
}
