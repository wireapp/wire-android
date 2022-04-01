package com.wire.android.ui.home.conversations

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

sealed class DeleteMessageState {
    data class State(
        val deleteMessageDialogState: DeleteMessageDialogState,
        val deleteMessageForYourselfDialogState: DeleteMessageDialogState
    ) : DeleteMessageState()

    data class Error(val coreFailure: CoreFailure) : DeleteMessageState()
}

sealed class DeleteMessageDialogState {
    object Hidden : DeleteMessageDialogState()
    data class Visible(
        val messageId: String,
        val conversationId: ConversationId,
        val loading: Boolean = false,
        val error: DeleteMessageError = DeleteMessageError.None
    ) : DeleteMessageDialogState()
}

sealed class DeleteMessageError {
    object None : DeleteMessageError()
    data class GenericError(val coreFailure: CoreFailure) : DeleteMessageError()
}
