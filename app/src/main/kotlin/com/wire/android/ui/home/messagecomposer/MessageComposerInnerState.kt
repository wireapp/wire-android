package com.wire.android.ui.home.messagecomposer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.getFileName
import com.wire.android.util.getMimeType
import com.wire.android.util.orDefault
import com.wire.android.util.toByteArray
import java.io.IOException

@Composable
fun rememberMessageComposerInnerState(
    fullScreenHeight: Dp,
    onMessageComposeInputStateChanged: (MessageComposerStateTransition) -> Unit
): MessageComposerInnerState {
    val defaultAttachmentInnerState = AttachmentInnerState(LocalContext.current)

    return remember {
        MessageComposerInnerState(
            fullScreenHeight= fullScreenHeight,
            attachmentInnerState = defaultAttachmentInnerState,
            onMessageComposeInputStateChanged = onMessageComposeInputStateChanged
        )
    }
}

class MessageComposerInnerState(
    val fullScreenHeight: Dp,
    val attachmentInnerState: AttachmentInnerState,
    private val onMessageComposeInputStateChanged: (MessageComposerStateTransition) -> Unit
) {

    var hasFocus by mutableStateOf(false)

    var isKeyboardShown by mutableStateOf(false)

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

class AttachmentInnerState(val context: Context) {
    var attachmentState by mutableStateOf<AttachmentState>(AttachmentState.NotPicked)

    suspend fun pickAttachment(attachmentUri: Uri) {
        attachmentState = try {
            val mimeType = attachmentUri.getMimeType(context).orDefault(DEFAULT_FILE_MIME_TYPE)
            val assetRawData = attachmentUri.toByteArray(context)
            val assetFileName = context.getFileName(attachmentUri)
            val attachmentType = if (mimeType.contains("image/")) AttachmentType.IMAGE else AttachmentType.GENERIC_FILE
            val attachment = AttachmentBundle(mimeType, assetRawData, assetFileName, attachmentType)
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

data class MessageComposerStateTransition(val from: MessageComposeInputState, val to: MessageComposeInputState)
