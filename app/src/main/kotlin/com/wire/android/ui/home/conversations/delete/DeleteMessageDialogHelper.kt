/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.conversations.delete

import com.wire.kalium.logic.data.id.QualifiedID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DeleteMessageDialogHelper(
    val scope: CoroutineScope,
    val conversationId: QualifiedID,
    private val updateDeleteDialogState: ((DeleteMessageDialogState) -> DeleteMessageDialogState) -> Unit,
    private val deleteMessage: suspend (messageId: String, deleteForEveryone: Boolean) -> Unit
) {

    private fun updateStateIfDialogVisible(newValue: (DeleteMessageDialogState.Visible) -> DeleteMessageDialogState) =
        updateDeleteDialogState {
            if (it is DeleteMessageDialogState.Visible) newValue(it) else it
        }

    fun showDeleteMessageForYourselfDialog(messageId: String) {
        updateDeleteDialogState {
            DeleteMessageDialogState.Visible(
                type = DeleteMessageDialogType.ForYourself,
                messageId = messageId,
                conversationId = conversationId,
            )
        }
    }

    fun onDeleteDialogDismissed() {
        updateDeleteDialogState {
            DeleteMessageDialogState.Hidden
        }
    }

    fun clearDeleteMessageError() {
        updateStateIfDialogVisible { it.copy(error = DeleteMessageError.None) }
    }

    fun onDeleteMessage(messageId: String, deleteForEveryone: Boolean) {
        scope.launch {
            updateStateIfDialogVisible {
                // update dialogs state to loading
                it.copy(loading = true)
            }

            deleteMessage(messageId, deleteForEveryone)

            onDeleteDialogDismissed()
        }
    }
}
