package com.wire.android.ui.home.conversations.delete

import com.wire.kalium.logic.data.id.QualifiedID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DeleteMessageDialogHelper(
    val scope: CoroutineScope,
    val conversationId: QualifiedID,
    private val updateDeleteDialogState: ((DeleteMessageDialogsState.States) -> DeleteMessageDialogsState) -> Unit,
    private val deleteMessage: suspend (String, Boolean) -> Unit
) {

    private fun updateStateIfDialogVisible(newValue: (DeleteMessageDialogActiveState.Visible) -> DeleteMessageDialogActiveState) =
        updateDeleteDialogState {
            when {
                it.forEveryone is DeleteMessageDialogActiveState.Visible -> it.copy(forEveryone = newValue(it.forEveryone))
                it.forYourself is DeleteMessageDialogActiveState.Visible -> it.copy(forYourself = newValue(it.forYourself))
                else -> it
            }
        }

    fun showDeleteMessageForYourselfDialog(messageId: String) {
        updateDeleteDialogState {
            it.copy(
                forEveryone = DeleteMessageDialogActiveState.Hidden,
                forYourself = DeleteMessageDialogActiveState.Visible(
                    messageId = messageId,
                    conversationId = conversationId
                )
            )
        }
    }

    fun onDeleteDialogDismissed() {
        updateDeleteDialogState {
            it.copy(
                forEveryone = DeleteMessageDialogActiveState.Hidden,
                forYourself = DeleteMessageDialogActiveState.Hidden
            )
        }
    }

    fun clearDeleteMessageError() {
        updateStateIfDialogVisible { it.copy(error = DeleteMessageError.None) }
    }

    fun onDeleteMessage(messageId: String, deleteForEveryone: Boolean) {
        scope.launch {
            // update dialogs state to loading
            if (deleteForEveryone) {
                updateDeleteDialogState {
                    it.copy(
                        forEveryone = DeleteMessageDialogActiveState.Visible(
                            messageId = messageId,
                            conversationId = conversationId,
                            loading = true
                        )
                    )
                }
            } else {
                updateDeleteDialogState {
                    it.copy(
                        forYourself = DeleteMessageDialogActiveState.Visible(
                            messageId = messageId,
                            conversationId = conversationId,
                            loading = true
                        )
                    )
                }
            }

            deleteMessage(messageId, deleteForEveryone)

            onDeleteDialogDismissed()
        }
    }
}
