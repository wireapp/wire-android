package com.wire.android.ui.home.conversations

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.QualifiedID as ConversationId

sealed class DeleteMessageDialogsState {
    data class States(
        val forEveryone: DeleteMessageDialogActiveState,
        val forYourself: DeleteMessageDialogActiveState
    ) : DeleteMessageDialogsState()

    data class Error(val coreFailure: CoreFailure) : DeleteMessageDialogsState()
}

sealed class DeleteMessageDialogActiveState {
    object Hidden : DeleteMessageDialogActiveState()
    data class Visible(
        val messageId: String,
        val conversationId: ConversationId,
        val loading: Boolean = false,
        val error: DeleteMessageError = DeleteMessageError.None
    ) : DeleteMessageDialogActiveState()
}

sealed class DeleteMessageError {
    object None : DeleteMessageError()
    data class GenericError(val coreFailure: CoreFailure) : DeleteMessageError()
}
