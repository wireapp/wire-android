package com.wire.android.ui.home.messagecomposer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.EMPTY
import com.wire.android.util.copyToTempPath
import com.wire.android.util.getFileName
import com.wire.android.util.getMimeType
import com.wire.android.util.orDefault
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.kalium.logic.data.asset.isDisplayableMimeType
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.Path
import okio.Path.Companion.toPath
import java.io.IOException
import java.util.UUID
import com.wire.android.util.WHITE_SPACE
import com.wire.kalium.logic.data.message.mention.MessageMention

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

    var mentionString by mutableStateOf("")

    var messageText by mutableStateOf(TextFieldValue(""))
        private set

    fun setMessageTextValue(text: TextFieldValue) {
        updateMentionsIfNeeded(text)
        requestMentionSuggestionIfNeeded(text)

        messageText = text
    }

    fun startMention() {
        val beforeSelection = messageText.text.subSequence(0, messageText.selection.min)
            .run {
                if (endsWith(String.WHITE_SPACE) || this == String.EMPTY) {
                    this.toString()
                } else {
                    StringBuilder(this)
                        .append(String.WHITE_SPACE)
                        .toString()
                }
            }
        val afterSelection = messageText.text.subSequence(messageText.selection.max, messageText.text.length)
        val resultText = StringBuilder(beforeSelection)
            .append("@")
            .append(afterSelection)
            .toString()
        val newSelection = TextRange(beforeSelection.length + 1)

        setMessageTextValue(TextFieldValue(resultText, newSelection))
    }

    fun addMention(contact: Contact) {
        val mention = UiMention(
            start = messageText.currentMentionStartIndex(),
            length = contact.name.length + 1, // + 1 cause there is an "@" before it
            userId = UserId(contact.id, contact.domain),
            handler = "@" + contact.name
        )

        addMentionIntoText(mention)
        mentions = mentions.plus(mention).sortedBy { it.start }
        _mentionQueryFlowState.value = null
    }

    private fun addMentionIntoText(mention: UiMention) {
        val beforeMention = messageText.text.subSequence(0, mention.start)
        val afterMention = messageText.text.subSequence(messageText.selection.max, messageText.text.length)
        val resultText = StringBuilder()
            .append(beforeMention)
            .append(mention.handler)
            .apply {
                if (!afterMention.startsWith(String.WHITE_SPACE))
                    append(String.WHITE_SPACE)
            }
            .append(afterMention)
            .toString()
        val newSelection = TextRange(beforeMention.length + mention.handler.length + 1)

        setMessageTextValue(TextFieldValue(resultText, newSelection))
    }

    var mentions by mutableStateOf(listOf<UiMention>())

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

    private fun updateMentionsIfNeeded(newText: TextFieldValue) {
        val updatedMentions = mutableSetOf<UiMention>()
        mentions.forEach { mention ->
            if (newText.text.length >= mention.start + mention.length) {
                val substringInMentionPlace = newText.text.substring(mention.start, mention.start + mention.length)
                if (substringInMentionPlace == mention.handler) {
                    updatedMentions.add(mention)
                    return@forEach
                }
            }

            val prevMentionEnd = updatedMentions.lastOrNull()?.let { it.start + it.length } ?: 0
            val newIndexOfMention = newText.text.indexOf(mention.handler, prevMentionEnd)
            if (newIndexOfMention >= 0)
                updatedMentions.add(mention.copy(start = newIndexOfMention))
        }

        mentions = updatedMentions.toList()
    }

    private fun requestMentionSuggestionIfNeeded(text: TextFieldValue) {
        if (text.selection.min != text.selection.max) {
            _mentionQueryFlowState.value = null
            return
        } else {
            mentions.firstOrNull { text.selection.min in it.start..it.start + it.length }?.let {
                _mentionQueryFlowState.value = null
                return
            }
        }

        val currentMentionStartIndex = text.currentMentionStartIndex()

        if (currentMentionStartIndex >= 0) {
            val sub = text.text.subSequence(currentMentionStartIndex, text.selection.min)
            if (!sub.contains(String.WHITE_SPACE)) {
                _mentionQueryFlowState.value = sub.toString()
                return
            }
        }
    }

}

private fun TextFieldValue.currentMentionStartIndex(): Int {
    val lastIndexOfAt = text.lastIndexOf("@", selection.min)

    return when {
        (lastIndexOfAt <= 0) ||
                (text[lastIndexOfAt - 1].toString() == String.WHITE_SPACE) -> lastIndexOfAt
        else -> -1
    }
}

data class UiMention(
    val start: Int,
    val length: Int,
    val userId: UserId,
    val handler: String // name that should be displayed in a message
) {
    fun intoMessageMention() = MessageMention(start, length, userId)
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
