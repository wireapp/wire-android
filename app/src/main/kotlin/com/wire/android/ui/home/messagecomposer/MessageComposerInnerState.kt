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
import androidx.compose.ui.unit.Dp
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.copyToTempPath
import com.wire.android.util.getFileName
import com.wire.android.util.getMimeType
import com.wire.android.util.orDefault
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.kalium.logic.data.asset.isDisplayableMimeType
import com.wire.kalium.logic.data.message.MessageMention
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.Path
import okio.Path.Companion.toPath
import java.io.IOException
import java.util.UUID

@Composable
fun rememberMessageComposerInnerState(
    fullScreenHeight: Dp,
    onMessageComposeInputStateChanged: (MessageComposerStateTransition) -> Unit
): MessageComposerInnerState {
    val defaultAttachmentInnerState = AttachmentInnerState(LocalContext.current)

    return remember {
        MessageComposerInnerState(
            fullScreenHeight = fullScreenHeight,
            attachmentInnerState = defaultAttachmentInnerState,
            onMessageComposeInputStateChanged = onMessageComposeInputStateChanged
        )
    }
}

data class MessageComposerInnerState(
    val fullScreenHeight: Dp,
    val attachmentInnerState: AttachmentInnerState,
    private val onMessageComposeInputStateChanged: (MessageComposerStateTransition) -> Unit
) {

    var hasFocus by mutableStateOf(false)

    var isKeyboardShown by mutableStateOf(false)

    var messageText by mutableStateOf(TextFieldValue(""))
        private set

    fun setMessageTextValue(text: TextFieldValue) {
        // TODO cyka calculate mentions here
        // TODO cyka don't forget about the case when big text was copy-passed into the field,
        //  need to check for mentions in that case too
        println("cyka ${text.selection}")
        messageText = text
    }

    fun startMention() {
        //TODO cyka add "@" to the place where cursor is
    }

    fun addMention(participant: UIParticipant) {
        //TODO cyka
        _mentionQueryFlowState.value = null
    }

    var mentions by mutableStateOf(listOf<MessageMention>())

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

    private val _mentionQueryFlowState: MutableStateFlow<String?> = MutableStateFlow(null)
    val mentionQueryFlowState: StateFlow<String?> = _mentionQueryFlowState

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

    suspend fun pickAttachment(attachmentUri: Uri, tempCachePath: Path) {
        attachmentState = try {
            val fullTempAssetPath = "$tempCachePath/${UUID.randomUUID()}".toPath()
            val assetFileName = context.getFileName(attachmentUri) ?: throw IOException("The selected asset has an invalid name")
            val mimeType = attachmentUri.getMimeType(context).orDefault(DEFAULT_FILE_MIME_TYPE)
            val attachmentType = if (isDisplayableMimeType(mimeType)) AttachmentType.IMAGE else AttachmentType.GENERIC_FILE
            val assetSize = if (attachmentType == AttachmentType.IMAGE)
                attachmentUri.resampleImageAndCopyToTempPath(context, fullTempAssetPath)
            else attachmentUri.copyToTempPath(context, fullTempAssetPath)
            val attachment = AttachmentBundle(mimeType, fullTempAssetPath, assetSize, assetFileName, attachmentType)
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
