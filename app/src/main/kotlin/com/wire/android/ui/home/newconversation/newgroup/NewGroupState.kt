package com.wire.android.ui.home.newconversation.newgroup

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.data.conversation.ConversationOptions

data class NewGroupState(
    val groupName: TextFieldValue = TextFieldValue(""),
    var groupProtocol: ConversationOptions.Protocol = ConversationOptions.Protocol.PROTEUS,
    val animatedGroupNameError: Boolean = false,
    val continueEnabled: Boolean = false,
    val isLoading : Boolean = false,
    val error: GroupNameError = GroupNameError.None
) {
    sealed class GroupNameError {
        object None : GroupNameError()
        sealed class TextFieldError : GroupNameError() {
            object GroupNameEmptyError : TextFieldError()
            object GroupNameExceedLimitError : TextFieldError()
        }
    }
}
