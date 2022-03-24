package com.wire.android.ui.home.messagecomposer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.home.conversations.model.AttachmentBundle

@Composable
fun rememberMessageComposerInnerState(
    defaultMessageText: String = "",
    defaultMessageComposeInputState: MessageComposeInputState = MessageComposeInputState.Enabled,
    defaultAttachmentState: AttachmentState = AttachmentState.NotPicked
): MessageComposerInnerState {

    return remember {
        MessageComposerInnerState(
            defaultMessageText,
            defaultMessageComposeInputState,
            defaultAttachmentState
        )
    }
}

class MessageComposerInnerState(
    defaultMessageText: String,
    defaultMessageComposeInputState: MessageComposeInputState,
    defaultAttachmentState: AttachmentState
) {

    var messageText by mutableStateOf(defaultMessageText)

    var messageComposeInputState by mutableStateOf(defaultMessageComposeInputState)
        private set

    val sendButtonEnabled: Boolean
        @Composable get() = if (messageComposeInputState == MessageComposeInputState.Enabled) {
            false
        } else {
            messageText.filter { !it.isWhitespace() }
                .isNotBlank()
        }

    var attachmentOptionsDisplayed by mutableStateOf(false)

    var attachmentState by mutableStateOf<AttachmentState>(defaultAttachmentState)

    private fun toEnabled() {
        messageComposeInputState = MessageComposeInputState.Enabled
    }

    fun clickOutSideMessageComposer() {
        if (messageText.filter { !it.isWhitespace() }.isBlank()) {
            toEnabled()
        }
    }

    fun toActive() {
        messageComposeInputState = MessageComposeInputState.Active
    }

    fun toggleFullScreen() {
        messageComposeInputState = if (messageComposeInputState == MessageComposeInputState.Active)
            MessageComposeInputState.FullScreen else MessageComposeInputState.Active
    }
}

enum class MessageComposeInputState {
    Active, Enabled, FullScreen
}

sealed class AttachmentState {
    object NotPicked : AttachmentState()
    class Picked(val attachmentBundle: AttachmentBundle) : AttachmentState()
    object Error : AttachmentState()
}
