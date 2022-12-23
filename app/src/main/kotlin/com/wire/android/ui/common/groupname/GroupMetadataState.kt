package com.wire.android.ui.common.groupname

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.kalium.logic.data.conversation.ConversationOptions

data class GroupMetadataState(
    val groupName: TextFieldValue = TextFieldValue(""),
    var groupProtocol: ConversationOptions.Protocol = ConversationOptions.Protocol.PROTEUS,
    val animatedGroupNameError: Boolean = false,
    val continueEnabled: Boolean = false,
    val mlsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val error: NewGroupError = NewGroupError.None,
    val mode: GroupNameMode = GroupNameMode.CREATION,
    val isSelfTeamMember: Boolean = false
) {
    sealed interface NewGroupError {
        object None : NewGroupError
        sealed interface TextFieldError : NewGroupError {
            object GroupNameEmptyError : TextFieldError
            object GroupNameExceedLimitError : TextFieldError
        }
    }
}

enum class GroupNameMode { CREATION, EDITION }
