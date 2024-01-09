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
