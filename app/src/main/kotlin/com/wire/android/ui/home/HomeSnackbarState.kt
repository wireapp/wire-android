/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home

// TODO change to extend [SnackBarMessage]
sealed class HomeSnackbarState {
    object None : HomeSnackbarState()
    data class ClearConversationContentSuccess(val isGroup: Boolean) : HomeSnackbarState()
    data class ClearConversationContentFailure(val isGroup: Boolean) : HomeSnackbarState()

    class SuccessConnectionIgnoreRequest(val userName: String) : HomeSnackbarState()
    object MutingOperationError : HomeSnackbarState()
    object BlockingUserOperationError : HomeSnackbarState()
    data class BlockingUserOperationSuccess(val userName: String) : HomeSnackbarState()
    object UnblockingUserOperationError : HomeSnackbarState()
    data class DeletedConversationGroupSuccess(val groupName: String) : HomeSnackbarState()
    object DeleteConversationGroupError : HomeSnackbarState()
    object LeftConversationSuccess : HomeSnackbarState()
    object LeaveConversationError : HomeSnackbarState()
    object ArchivingConversationSuccess : HomeSnackbarState()
    object ArchivingConversationError : HomeSnackbarState()
}
