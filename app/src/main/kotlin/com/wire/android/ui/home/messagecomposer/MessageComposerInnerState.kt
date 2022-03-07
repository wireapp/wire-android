package com.wire.android.ui.home.messagecomposer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun rememberMessageComposerInnerState(
    defaultMessageText: String = "",
    defaultMessageComposeInputState: MessageComposeInputState = MessageComposeInputState.Enabled
): MessageComposerInnerState {

    return remember {
        MessageComposerInnerState(
            defaultMessageText,
            defaultMessageComposeInputState
        )
    }
}

class MessageComposerInnerState(
    defaultMessageText: String,
    defaultMessageComposeInputState: MessageComposeInputState,
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
