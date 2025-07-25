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

package com.wire.android.ui.home

import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.util.ui.UIText

sealed class HomeSnackBarMessage(override val uiText: UIText) : SnackBarMessage {

    data class ClearConversationContentSuccess(val isGroup: Boolean) : HomeSnackBarMessage(
        UIText.StringResource(R.string.conversation_content_deleted)
    )

    data class ClearConversationContentFailure(val isGroup: Boolean) : HomeSnackBarMessage(
        UIText.StringResource(R.string.conversation_content_delete_failure)
    )

    class SuccessConnectionIgnoreRequest(val userName: String) :
        HomeSnackBarMessage(UIText.StringResource(R.string.connection_request_ignored, userName))

    data object MutingOperationError : HomeSnackBarMessage(UIText.StringResource(R.string.error_updating_muting_setting))
    data object BlockingUserOperationError : HomeSnackBarMessage(UIText.StringResource(R.string.error_blocking_user))
    data class BlockingUserOperationSuccess(val userName: String) :
        HomeSnackBarMessage(UIText.StringResource(R.string.blocking_user_success, userName))
    data class UnblockingUserOperationSuccess(val userName: String) :
        HomeSnackBarMessage(UIText.StringResource(R.string.unblocking_user_success, userName))

    data object UnblockingUserOperationError : HomeSnackBarMessage(UIText.StringResource(R.string.error_unblocking_user))
    data class DeletedConversationGroupSuccess(val groupName: String) : HomeSnackBarMessage(
        UIText.StringResource(
            R.string.conversation_removed_success,
            groupName
        )
    )
    data class DeleteConversationGroupLocallySuccess(val groupName: String) : HomeSnackBarMessage(
        UIText.StringResource(
            R.string.conversation_removed_locally_success,
            groupName
        )
    )

    data object DeleteConversationGroupError : HomeSnackBarMessage(UIText.StringResource(R.string.delete_conversation_conversation_error))
    data object LeftConversationSuccess : HomeSnackBarMessage(UIText.StringResource(R.string.left_conversation_success))
    data object LeaveConversationError : HomeSnackBarMessage(UIText.StringResource(R.string.leave_conversation_error))
    data class UpdateArchivingStatusSuccess(val isArchiving: Boolean) : HomeSnackBarMessage(
        UIText.StringResource(
            if (isArchiving) {
                R.string.success_archiving_conversation
            } else {
                R.string.success_unarchiving_conversation
            }
        )
    )

    data class UpdateArchivingStatusError(val isArchiving: Boolean) : HomeSnackBarMessage(
        UIText.StringResource(
            if (isArchiving) {
                R.string.error_archiving_conversation
            } else {
                R.string.error_unarchiving_conversation
            }
        )
    )

    data class UpdateFavoriteStatusSuccess(val name: String, val addingToFavorite: Boolean) : HomeSnackBarMessage(
        UIText.StringResource(if (addingToFavorite) R.string.success_adding_to_favorite else R.string.success_removing_from_favorite, name)
    )
    data class UpdateFavoriteStatusError(val name: String, val addingToFavorite: Boolean) : HomeSnackBarMessage(
        UIText.StringResource(if (addingToFavorite) R.string.error_adding_to_favorite else R.string.error_removing_from_favorite, name)
    )
    data class RemoveFromFolderSuccess(val name: String, val folder: String) : HomeSnackBarMessage(
        UIText.StringResource(R.string.remove_from_folder_success, name, folder)
    )
    data class RemoveFromFolderError(val name: String) : HomeSnackBarMessage(
        UIText.StringResource(R.string.remove_from_folder_failed, name)
    )
}
