package com.wire.android.ui.home.messagecomposer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

class MessageComposerState(
    defaultMessageText: TextFieldValue,
    defaultMessageComposeInputState: MessageComposeInputState,
) {

    var messageText by mutableStateOf(defaultMessageText)

    var messageComposeInputState by mutableStateOf(defaultMessageComposeInputState)
        private set

    val sendButtonEnabled: Boolean
        @Composable get() = if (messageComposeInputState == MessageComposeInputState.Enabled) {
            false
        } else {
            messageText.text.filter { !it.isWhitespace() }
                .isNotBlank()
        }

    val isEnabled: Boolean = messageComposeInputState == MessageComposeInputState.Enabled

    val isActive: Boolean = messageComposeInputState == MessageComposeInputState.Active

    val isFullScreen: Boolean = messageComposeInputState == MessageComposeInputState.FullScreen

    fun toEnabled() {
        messageComposeInputState = MessageComposeInputState.Enabled
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
