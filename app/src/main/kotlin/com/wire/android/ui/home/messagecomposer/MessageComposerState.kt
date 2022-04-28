package com.wire.android.ui.home.messagecomposer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.home.messagecomposer.attachment.AttachmentState


class MessageComposerState(
    val fullScreenHeight: Dp,
    val attachmentState: AttachmentState,
    private val onMessageComposeInputStateChanged: (MessageComposerStateTransition) -> Unit
) {

    var hasFocus by mutableStateOf(false)

    var messageText by mutableStateOf(TextFieldValue(""))

    var messageComposeInputState by mutableStateOf(MessageComposeInputState.Enabled)
        private set

    val sendButtonEnabled: Boolean
        @Composable get() = if (messageComposeInputState == MessageComposeInputState.Enabled) {
            false
        } else {
            messageText.text.filter { !it.isWhitespace() }
                .isNotBlank()
        }

    var attachmentOptionsDisplayed by mutableStateOf(false)
        private set

    fun toggleAttachmentOptionsVisibility() {
        attachmentOptionsDisplayed = !attachmentOptionsDisplayed
    }

    private fun toEnabled() {
        onMessageComposeInputStateChanged(
            MessageComposerStateTransition(
                from = messageComposeInputState,
                to = MessageComposeInputState.Enabled
            )
        )
        messageComposeInputState = MessageComposeInputState.Enabled
    }

    fun clickOutSideMessageComposer() {
        if (messageText.text.filter { !it.isWhitespace() }.isBlank()) {
            toEnabled()
        }
    }

    fun toActive() {
        onMessageComposeInputStateChanged(
            MessageComposerStateTransition(
                from = messageComposeInputState,
                to = MessageComposeInputState.Active
            )
        )

        hasFocus = true
        attachmentOptionsDisplayed = false
        messageComposeInputState = MessageComposeInputState.Active
    }

    fun toggleFullScreen() {
        val newState = if (messageComposeInputState == MessageComposeInputState.Active)
            MessageComposeInputState.FullScreen else MessageComposeInputState.Active

        onMessageComposeInputStateChanged(
            MessageComposerStateTransition(
                from = messageComposeInputState,
                to = newState
            )
        )

        messageComposeInputState = newState
    }
}

@Composable
fun rememberMessageComposerInnerState(
    fullScreenHeight: Dp,
    onMessageComposeInputStateChanged: (MessageComposerStateTransition) -> Unit
): MessageComposerState {
    val defaultAttachmentState = AttachmentState(LocalContext.current)

    return remember {
        MessageComposerState(
            fullScreenHeight = fullScreenHeight,
            attachmentState = defaultAttachmentState,
            onMessageComposeInputStateChanged = onMessageComposeInputStateChanged
        )
    }
}

enum class MessageComposeInputState {
    Active, Enabled, FullScreen
}

data class MessageComposerStateTransition(val from: MessageComposeInputState, val to: MessageComposeInputState)
