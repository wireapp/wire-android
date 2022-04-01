package com.wire.android.ui.home.messagecomposer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.getMimeType
import com.wire.android.util.orDefault
import com.wire.android.util.toByteArray
import java.io.IOException

@Composable
fun rememberMessageComposerInnerState(): MessageComposerInnerState {
    val defaultAttachmentInnerState = AttachmentInnerState(LocalContext.current)

    return remember {
        MessageComposerInnerState(defaultAttachmentInnerState)
    }
}

class MessageComposerInnerState(
    val attachmentInnerState: AttachmentInnerState
) {

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

    private fun toEnabled() {
        messageComposeInputState = MessageComposeInputState.Enabled
    }

    fun clickOutSideMessageComposer() {
        if (messageText.text.filter { !it.isWhitespace() }.isBlank()) {
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

class AttachmentInnerState(val context: Context) {
    var attachmentState by mutableStateOf<AttachmentState>(AttachmentState.NotPicked)

    suspend fun pickAttachment(attachmentUri: Uri) {
        attachmentState = try {
            val attachment =
                AttachmentBundle(
                    attachmentUri.getMimeType(context).orDefault(DEFAULT_FILE_MIME_TYPE),
                    attachmentUri.toByteArray(context)
                )
            AttachmentState.Picked(attachment)
        } catch (e: IOException) {
            appLogger.e("There was an error while obtaining the file from disk", e)
            AttachmentState.Error
        }
    }

    fun resetAttachmentState() {
        attachmentState = AttachmentState.NotPicked
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
