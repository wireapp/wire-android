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

    // Conversation BottomSheet
    object BlockingUserOperationError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.error_blocking_user))
    class BlockingUserOperationSuccess(val name: String) :
        OtherUserProfileInfoMessageType(UIText.StringResource(R.string.blocking_user_success, name))

    object MutingOperationError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.error_updating_muting_setting))

    object UnblockingUserOperationError : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.error_unblocking_user))

    object ConversationContentDeleted : OtherUserProfileInfoMessageType(UIText.StringResource(R.string.conversation_content_deleted))

    object ConversationContentDeleteFailure :
        OtherUserProfileInfoMessageType(UIText.StringResource(R.string.conversation_content_delete_failure))

}
