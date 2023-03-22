/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.messagecomposer

import android.content.Context
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.ui.home.conversations.model.QuotedMessageUIData
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.EMPTY
import com.wire.android.util.FileManager
import com.wire.android.util.MENTION_SYMBOL
import com.wire.android.util.NEW_LINE_SYMBOL
import com.wire.android.util.WHITE_SPACE
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getFileName
import com.wire.android.util.getMimeType
import com.wire.android.util.orDefault
import com.wire.android.util.resampleImageAndCopyToTempPath
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import java.io.IOException
import java.util.UUID

@Composable
fun rememberMessageComposerInnerState(): MessageComposerInnerState {
    val context = LocalContext.current

    val defaultAttachmentInnerState = AttachmentInnerState(context)

    val mentionSpanStyle = SpanStyle(
        color = MaterialTheme.wireColorScheme.onPrimaryVariant,
        background = MaterialTheme.wireColorScheme.primaryVariant
    )

    val focusManager = LocalFocusManager.current
    val inputFocusRequester = FocusRequester()

    return remember {
        MessageComposerInnerState(
            context = context,
            focusManager = focusManager,
            inputFocusRequester = inputFocusRequester,
            attachmentInnerState = defaultAttachmentInnerState,
            mentionSpanStyle = mentionSpanStyle
        )
    }
}

@Suppress("TooManyFunctions")
data class MessageComposerInnerState(
    val context: Context,
    val attachmentInnerState: AttachmentInnerState,
    val focusManager: FocusManager,
    val inputFocusRequester: FocusRequester,
    private val mentionSpanStyle: SpanStyle
) {
    var messageComposeInputState: MessageComposeInputState by mutableStateOf(MessageComposeInputState.Inactive())
        private set
    private val _mentionQueryFlowState: MutableStateFlow<String?> = MutableStateFlow(null)

    val mentionQueryFlowState: StateFlow<String?> = _mentionQueryFlowState

    var mentions by mutableStateOf(listOf<UiMention>())

    var quotedMessageData: QuotedMessageUIData? by mutableStateOf(null)

    fun setMessageTextValue(text: TextFieldValue) {
        updateMentionsIfNeeded(text)
        requestMentionSuggestionIfNeeded(text)

        messageComposeInputState = messageComposeInputState.copyCurrent(messageText = applyMentionStylesIntoText(text))
    }

    fun startMention() {
        val beforeSelection = messageComposeInputState.messageText.text
            .subSequence(0, messageComposeInputState.messageText.selection.min)
            .run {
                if (endsWith(String.WHITE_SPACE) || endsWith(String.NEW_LINE_SYMBOL) || this == String.EMPTY) {
                    this.toString()
                } else {
                    StringBuilder(this)
                        .append(String.WHITE_SPACE)
                        .toString()
                }
            }
        val afterSelection = messageComposeInputState.messageText.text
            .subSequence(messageComposeInputState.messageText.selection.max, messageComposeInputState.messageText.text.length)
        val resultText = StringBuilder(beforeSelection)
            .append(String.MENTION_SYMBOL)
            .append(afterSelection)
            .toString()
        val newSelection = TextRange(beforeSelection.length + 1)

        setMessageTextValue(TextFieldValue(resultText, newSelection))
    }

    fun addMention(contact: Contact) {
        val mention = UiMention(
            start = messageComposeInputState.messageText.currentMentionStartIndex(),
            length = contact.name.length + 1, // +1 cause there is an "@" before it
            userId = UserId(contact.id, contact.domain),
            handler = String.MENTION_SYMBOL + contact.name
        )

        insertMentionIntoText(mention)
        mentions = mentions.plus(mention).sortedBy { it.start }
        _mentionQueryFlowState.value = null
    }

    fun toInactive(clearInput: Boolean = false) {
        if (clearInput) {
            messageComposeInputState = messageComposeInputState.toInactive(messageText = TextFieldValue(""))
        } else if (messageComposeInputState !is MessageComposeInputState.Inactive) {
            messageComposeInputState = messageComposeInputState.toInactive()
        }
    }

    fun toActive() {
        if (messageComposeInputState !is MessageComposeInputState.Active) {
            messageComposeInputState = messageComposeInputState.toActive()
        }
    }

    fun toEditMessage(messageId: String, originalText: String) {
        messageComposeInputState = MessageComposeInputState.Active(
            messageText = TextFieldValue(text = originalText, selection = TextRange(originalText.length)),
            type = MessageComposeInputType.EditMessage(messageId, originalText)
        )
        quotedMessageData = null
        inputFocusRequester.requestFocus()
    }

    fun showAttachmentOptions() = changeAttachmentOptionsVisibility(true)
    fun hideAttachmentOptions() = changeAttachmentOptionsVisibility(false)
    private fun changeAttachmentOptionsVisibility(newValue: Boolean) {
        (messageComposeInputState as? MessageComposeInputState.Active)?.let { activeState ->
            (activeState.type as? MessageComposeInputType.NewMessage)?.let { newMessageType ->
                messageComposeInputState = activeState.copy(
                    type = newMessageType.copy(
                        attachmentOptionsDisplayed = newValue
                    )
                )
            }
        }
    }

    fun messageComposeInputFocusChange(isFocused: Boolean) {
        messageComposeInputState = messageComposeInputState.copyCurrent(inputFocused = isFocused)
    }

    fun toggleFullScreen() {
        (messageComposeInputState as? MessageComposeInputState.Active)?.let {
            messageComposeInputState = it.copy(
                size = when (it.size) {
                    MessageComposeInputSize.COLLAPSED -> MessageComposeInputSize.EXPANDED
                    MessageComposeInputSize.EXPANDED -> MessageComposeInputSize.COLLAPSED
                }
            )
        }
    }

    private fun applyMentionStylesIntoText(text: TextFieldValue): TextFieldValue {
        // For now there is a known issue in Compose
        // https://issuetracker.google.com/issues/199768107
        // It do not allow us to set some custom SpanStyle into "EditableTextView" :(
        // But maybe someday they'll fix it, so we could use it
        val spanStyles = mentions.map { mention ->
            AnnotatedString.Range(mentionSpanStyle, mention.start, mention.start + mention.length)
        }

//        return text.copy(
//            annotatedString = AnnotatedString(
//                text.annotatedString.text,
//                spanStyles,
//                text.annotatedString.paragraphStyles
//            )
//        )
        return text
    }

    private fun insertMentionIntoText(mention: UiMention) {
        val beforeMentionText = messageComposeInputState.messageText.text
            .subSequence(0, mention.start)
        val afterMentionText = messageComposeInputState.messageText.text
            .subSequence(messageComposeInputState.messageText.selection.max, messageComposeInputState.messageText.text.length)
        val resultText = StringBuilder()
            .append(beforeMentionText)
            .append(mention.handler)
            .apply {
                if (!afterMentionText.startsWith(String.WHITE_SPACE)) append(String.WHITE_SPACE)
            }
            .append(afterMentionText)
            .toString()

        // + 1 cause we add space after mention and move selector there
        val newSelection = TextRange(beforeMentionText.length + mention.handler.length + 1)

        setMessageTextValue(TextFieldValue(resultText, newSelection))
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
            if (newIndexOfMention >= 0) {
                updatedMentions.add(mention.copy(start = newIndexOfMention))
            }
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
            // +1 cause need to remove @ symbol at the begin of string
            val textBetweenAtAndSelection = text.text.subSequence(currentMentionStartIndex + 1, text.selection.min)
            if (!textBetweenAtAndSelection.contains(String.WHITE_SPACE)) {
                _mentionQueryFlowState.value = textBetweenAtAndSelection.toString()
            } else {
                _mentionQueryFlowState.value = null
            }
        } else {
            _mentionQueryFlowState.value = null
        }
    }

    fun reply(uiMessage: UIMessage) {
        val authorName = uiMessage.messageHeader.username
        val authorId = uiMessage.messageHeader.userId ?: return

        val content = when (val content = uiMessage.messageContent) {
            is UIMessageContent.AssetMessage -> QuotedMessageUIData.GenericAsset(
                assetName = content.assetName,
                assetMimeType = content.assetExtension
            )

            is UIMessageContent.RestrictedAsset -> QuotedMessageUIData.GenericAsset(
                assetName = content.assetName,
                assetMimeType = content.mimeType
            )

            is UIMessageContent.TextMessage -> QuotedMessageUIData.Text(
                value = content.messageBody.message.asString(context.resources)
            )

            is UIMessageContent.AudioAssetMessage -> QuotedMessageUIData.AudioMessage

            is UIMessageContent.ImageMessage -> content.asset?.let {
                QuotedMessageUIData.DisplayableImage(displayable = content.asset)
            }

            else -> {
                appLogger.w("Attempting to reply to an unsupported message type of content = $content")
                null
            }
        }
        content?.let { quotedContent ->
            quotedMessageData = QuotedMessageUIData(
                messageId = uiMessage.messageHeader.messageId,
                senderId = authorId,
                senderName = authorName,
                originalMessageDateDescription = "".toUIText(),
                editedTimeDescription = "".toUIText(),
                quotedContent = quotedContent
            )
        }
        toActive()
    }

    fun cancelReply() {
        quotedMessageData = null
    }
}

private fun TextFieldValue.currentMentionStartIndex(): Int {
    val lastIndexOfAt = text.lastIndexOf(String.MENTION_SYMBOL, selection.min - 1)

    return when {
        (lastIndexOfAt <= 0) ||
                (text[lastIndexOfAt - 1].toString() in listOf(String.WHITE_SPACE, String.NEW_LINE_SYMBOL)) -> lastIndexOfAt

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

    suspend fun pickAttachment(
        attachmentUri: Uri,
        tempCachePath: Path,
        dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
    ) = withContext(dispatcherProvider.io()) {
        val fileManager = FileManager(context)
        attachmentState = try {
            val fullTempAssetPath = "$tempCachePath/${UUID.randomUUID()}".toPath()
            val assetFileName = context.getFileName(attachmentUri) ?: throw IOException("The selected asset has an invalid name")
            val mimeType = attachmentUri.getMimeType(context).orDefault(DEFAULT_FILE_MIME_TYPE)
            val attachmentType = AttachmentType.fromMimeTypeString(mimeType)
            val assetSize = if (attachmentType == AttachmentType.IMAGE) {
                attachmentUri.resampleImageAndCopyToTempPath(context, fullTempAssetPath)
            } else {
                fileManager.copyToTempPath(attachmentUri, fullTempAssetPath)
            }
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

@Stable
sealed class MessageComposeInputState {
    abstract val messageText: TextFieldValue
    abstract val inputFocused: Boolean

    @Stable
    data class Inactive(
        override val messageText: TextFieldValue = TextFieldValue(""),
        override val inputFocused: Boolean = false,
    ) : MessageComposeInputState()

    @Stable
    data class Active(
        override val messageText: TextFieldValue = TextFieldValue(""),
        override val inputFocused: Boolean = false,
        val type: MessageComposeInputType = MessageComposeInputType.NewMessage(),
        val size: MessageComposeInputSize = MessageComposeInputSize.COLLAPSED,
    ) : MessageComposeInputState()

    fun toActive(messageText: TextFieldValue = this.messageText, inputFocused: Boolean = this.inputFocused) = when (this) {
        is Active -> Active(messageText, inputFocused, this.type, this.size)
        is Inactive -> Active(messageText, inputFocused)
    }

    fun toInactive(messageText: TextFieldValue = this.messageText, inputFocused: Boolean = this.inputFocused) =
        Inactive(messageText, inputFocused)

    fun copyCurrent(messageText: TextFieldValue = this.messageText, inputFocused: Boolean = this.inputFocused) = when (this) {
        is Active -> Active(messageText, inputFocused, this.type, this.size)
        is Inactive -> Inactive(messageText, inputFocused)
    }

    val isExpanded: Boolean
        get() = this is Active && this.size == MessageComposeInputSize.EXPANDED
    val isEditMessage: Boolean
        get() = this is Active && this.type is MessageComposeInputType.EditMessage
    val isNewMessage: Boolean
        get() = this is Active && this.type is MessageComposeInputType.NewMessage
    val attachmentOptionsDisplayed: Boolean
        get() = this is Active && this.type is MessageComposeInputType.NewMessage && this.type.attachmentOptionsDisplayed
    val sendButtonEnabled: Boolean
        get() = this is Active && this.type is MessageComposeInputType.NewMessage && messageText.text.trim().isNotBlank()
    val editSaveButtonEnabled: Boolean
        get() = this is Active && this.type is MessageComposeInputType.EditMessage && messageText.text.trim().isNotBlank()
                && messageText.text.trim() != this.type.originalText.trim()
}

enum class MessageComposeInputSize {
    COLLAPSED, // wrap content
    EXPANDED; // fullscreen
}

@Stable
sealed class MessageComposeInputType {

    @Stable
    data class NewMessage(
        val attachmentOptionsDisplayed: Boolean = false,
    ) : MessageComposeInputType()

    @Stable
    data class EditMessage(
        val messageId: String,
        val originalText: String,
    ) : MessageComposeInputType()
}

sealed class AttachmentState {
    object NotPicked : AttachmentState()
    class Picked(val attachmentBundle: AttachmentBundle) : AttachmentState()
    object Error : AttachmentState()
}
