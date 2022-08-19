package com.wire.android.ui.common.topappbar

import com.wire.kalium.logic.data.id.ConversationId

data class ConnectivityUIState(
    val info: Info
) {

    sealed interface Info {
        object Connecting : Info

        object WaitingConnection : Info

        object None : Info

        data class EstablishedCall(
            val conversationId: ConversationId,
            val isMuted: Boolean
        ) : Info
    }
}
