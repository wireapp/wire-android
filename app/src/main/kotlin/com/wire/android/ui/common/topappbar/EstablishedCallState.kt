package com.wire.android.ui.common.topappbar

import com.wire.kalium.logic.data.id.ConversationId

data class EstablishedCallState(
    val conversationId: ConversationId? = null,
    val isCallHappening: Boolean = false,
    val shouldShow: Boolean = false,
    val isMuted: Boolean? = null
)
