package com.wire.android.ui.home.newconversation.newgroup

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.data.conversation.ConversationOptions

data class NewGroupState(
    val groupName: TextFieldValue = TextFieldValue(""),
    var groupProtocol: ConversationOptions.Protocol = ConversationOptions.Protocol.PROTEUS,
    val animatedGroupNameError: Boolean = false,
    val continueEnabled: Boolean = false,
    val mlsEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: NewGroupError = NewGroupError.None
) {
    sealed interface NewGroupError {
        object None : NewGroupError
        sealed interface TextFieldError : NewGroupError {
            object GroupNameEmptyError : TextFieldError
            object GroupNameExceedLimitError : TextFieldError
        }
    }
}
