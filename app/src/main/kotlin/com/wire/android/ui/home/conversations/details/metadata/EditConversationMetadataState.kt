package com.wire.android.ui.home.conversations.details.metadata

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.newconversation.newgroup.NewGroupState

data class EditConversationMetadataState(
    val groupName: TextFieldValue = TextFieldValue(""),
    val animatedGroupNameError: Boolean = false,
    val continueEnabled: Boolean = false,
    val error: NewGroupState.NewGroupError = NewGroupState.NewGroupError.None
)
