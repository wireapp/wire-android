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

package com.wire.android.ui.userprofile.other

import com.wire.android.R
import com.wire.android.model.SnackBarMessage
import com.wire.android.util.ui.UIText

sealed class OtherUserProfileInfoMessageType(override val uiText: UIText) : SnackBarMessage {
    // connection
    object SuccessConnectionSentRequest : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.connection_request_sent))
    object ConnectionRequestError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.connection_request_sent_error))
    object SuccessConnectionAcceptRequest : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.connection_request_accepted))
    object ConnectionAcceptError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.connection_request_accept_error))
    object SuccessConnectionCancelRequest : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.connection_request_canceled))
    object ConnectionCancelError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.connection_request_cancel_error))
    object ConnectionIgnoreError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.connection_request_ignore_error))

    object LoadUserInformationError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.error_unknown_message))
    object LoadDirectConversationError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.error_unknown_message))

    // change group role
    object ChangeGroupRoleError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.user_profile_role_change_error))

    // remove conversation member
    object RemoveConversationMemberError :
        OtherUserProfileInfoMessageType(UIText.StringResource(R.string.dialog_remove_conversation_member_error))
    data class RemoveConversationMemberSuccess(val userName: String) :
        OtherUserProfileInfoMessageType(UIText.PluralResource(R.plurals.label_system_message_federation_member_removed, 1, userName))

    // Conversation BottomSheet
    object BlockingUserOperationError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.error_blocking_user))
    class BlockingUserOperationSuccess(val name: String) :
        OtherUserProfileInfoMessageType(UIText.StringResource(R.string.blocking_user_success, name))

    object MutingOperationError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.error_updating_muting_setting))
    object UnblockingUserOperationError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.error_unblocking_user))
    object ConversationContentDeleted : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.conversation_content_deleted))
    object ConversationContentDeleteFailure :
        OtherUserProfileInfoMessageType(UIText.StringResource(R.string.conversation_content_delete_failure))

    data class ArchiveConversationError(val isArchiving: Boolean) : OtherUserProfileInfoMessageType(
        UIText.StringResource(
            if (isArchiving) R.string.error_archiving_conversation
            else R.string.error_unarchiving_conversation
        )
    )

    data class ArchiveConversationSuccess(val isArchiving: Boolean) : OtherUserProfileInfoMessageType(
        UIText.StringResource(
            if (isArchiving) R.string.success_archiving_conversation
            else R.string.success_unarchiving_conversation
        )
    )
}
