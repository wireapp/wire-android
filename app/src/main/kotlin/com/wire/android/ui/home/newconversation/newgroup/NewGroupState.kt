package com.wire.android.ui.home.newconversation.newgroup

import androidx.compose.ui.text.input.TextFieldValue

data class NewGroupState(
    val groupName: TextFieldValue = TextFieldValue(""),
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
