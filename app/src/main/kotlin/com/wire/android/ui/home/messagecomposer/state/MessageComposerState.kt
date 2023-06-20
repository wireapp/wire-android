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

package com.wire.android.ui.home.messagecomposer.state

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.messagecomposer.model.UiMention
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.EMPTY
import com.wire.android.util.MENTION_SYMBOL
import com.wire.android.util.NEW_LINE_SYMBOL
import com.wire.android.util.WHITE_SPACE
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration.Companion.ZERO

@Composable
fun rememberMessageComposerState(): MessageComposerState {
    val context = LocalContext.current

    val mentionSpanStyle = SpanStyle(
        color = MaterialTheme.wireColorScheme.onPrimaryVariant,
        background = MaterialTheme.wireColorScheme.primaryVariant
    )

    val focusManager = LocalFocusManager.current
    val inputFocusRequester = FocusRequester()

    return remember {
        MessageComposerState(
            context = context,
            focusManager = focusManager,
            inputFocusRequester = inputFocusRequester,
            mentionSpanStyle = mentionSpanStyle
        )
    }
}

@Suppress("TooManyFunctions")
data class MessageComposerState(
    val context: Context,
    val focusManager: FocusManager,
    val inputFocusRequester: FocusRequester,
    private val mentionSpanStyle: SpanStyle
) {
    var messageComposeInputState: MessageComposeInputState by mutableStateOf(MessageComposeInputState.Inactive())
        private set

    private var currentSelfDeletionTimer: SelfDeletionTimer by mutableStateOf(SelfDeletionTimer.Enabled(ZERO))

    private val _mentionQueryFlowState: MutableStateFlow<String?> = MutableStateFlow(null)

    val mentionQueryFlowState: StateFlow<String?> = _mentionQueryFlowState

    var mentions by mutableStateOf(listOf<UiMention>())

    var quotedMessageData: UIQuotedMessage.UIQuotedData? by mutableStateOf(null)

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
            .subSequence(
                messageComposeInputState.messageText.selection.max,
                messageComposeInputState.messageText.text.length
            )
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

    fun closeEditToInactive() {
        focusManager.clearFocus()
        setMessageTextValue(TextFieldValue(""))
        toInactive()
    }

    fun toInactive() {
        if (messageComposeInputState !is MessageComposeInputState.Inactive) {
            messageComposeInputState = messageComposeInputState.toInactive()
        }
    }

    fun toActive() {
        if (messageComposeInputState !is MessageComposeInputState.Active) {
            messageComposeInputState = messageComposeInputState.toActive(selfDeletionTimer = currentSelfDeletionTimer)
        }
    }

    fun toEditMessage(messageId: String, originalText: String, originalMentions: List<MessageMention>) {
        messageComposeInputState = MessageComposeInputState.Active(
            messageText = TextFieldValue(text = originalText, selection = TextRange(originalText.length)),
            type = MessageComposeInputType.EditMessage(messageId, originalText)
        )
        mentions = originalMentions.map { it.toUiMention(originalText) }
        quotedMessageData = null
        inputFocusRequester.requestFocus()
    }

    fun showRichTextEditingOptions() = changeRichTextEditingOptionsVisibility(true)
    fun hideRichTextEditingOptions() = changeRichTextEditingOptionsVisibility(false)

    private fun changeRichTextEditingOptionsVisibility(show: Boolean) {
        (messageComposeInputState as? MessageComposeInputState.Active)?.let { activeState ->
            when (val currentType = activeState.type) {
                is MessageComposeInputType.NewMessage -> {
                    messageComposeInputState = activeState.copy(
                        type = currentType.copy(
                            richTextFormattingOptionsDisplayed = show,
                            attachmentOptionsDisplayed = false
                        )
                    )
                }

                is MessageComposeInputType.EditMessage -> {
                    messageComposeInputState = activeState.copy(
                        type = currentType.copy(
                            richTextFormattingOptionsDisplayed = show
                        )
                    )
                }

                is MessageComposeInputType.SelfDeletingMessage -> {
                    messageComposeInputState = activeState.copy(
                        type = currentType.copy(
                            richTextFormattingOptionsDisplayed = show,
                            attachmentOptionsDisplayed = false
                        )
                    )
                }
            }
        }
    }

    fun showAttachmentOptions() = changeAttachmentOptionsVisibility(true)
    fun hideAttachmentOptions() = changeAttachmentOptionsVisibility(false)
    private fun changeAttachmentOptionsVisibility(newValue: Boolean) {
        (messageComposeInputState as? MessageComposeInputState.Active)?.let { activeState ->
            when (val currentType = activeState.type) {
                is MessageComposeInputType.NewMessage -> {
                    messageComposeInputState = activeState.copy(
                        type = currentType.copy(
                            attachmentOptionsDisplayed = newValue,
                            richTextFormattingOptionsDisplayed = false
                        )
                    )
                }

                is MessageComposeInputType.SelfDeletingMessage -> {
                    messageComposeInputState = activeState.copy(
                        type = currentType.copy(
                            attachmentOptionsDisplayed = newValue,
                            richTextFormattingOptionsDisplayed = false
                        )
                    )
                }

                else -> {}
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
            .subSequence(
                messageComposeInputState.messageText.selection.max,
                messageComposeInputState.messageText.text.length
            )
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

    fun reply(uiMessage: UIMessage.Regular) {
        val authorName = uiMessage.header.username
        val authorId = uiMessage.header.userId ?: return

        val content = when (val content = uiMessage.messageContent) {
            is UIMessageContent.AssetMessage -> UIQuotedMessage.UIQuotedData.GenericAsset(
                assetName = content.assetName,
                assetMimeType = content.assetExtension
            )

            is UIMessageContent.RestrictedAsset -> UIQuotedMessage.UIQuotedData.GenericAsset(
                assetName = content.assetName,
                assetMimeType = content.mimeType
            )

            is UIMessageContent.TextMessage -> UIQuotedMessage.UIQuotedData.Text(
                value = content.messageBody.message.asString(context.resources)
            )

            is UIMessageContent.AudioAssetMessage -> UIQuotedMessage.UIQuotedData.AudioMessage

            is UIMessageContent.ImageMessage -> content.asset?.let {
                UIQuotedMessage.UIQuotedData.DisplayableImage(displayable = content.asset)
            }

            else -> {
                appLogger.w("Attempting to reply to an unsupported message type of content = $content")
                null
            }
        }
        content?.let { quotedContent ->
            quotedMessageData = UIQuotedMessage.UIQuotedData(
                messageId = uiMessage.header.messageId,
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

    fun updateSelfDeletionTime(newSelfDeletionTimer: SelfDeletionTimer) = with(newSelfDeletionTimer) {
        currentSelfDeletionTimer = newSelfDeletionTimer
        val newSelfDeletionDuration = newSelfDeletionTimer.toDuration().toSelfDeletionDuration()
        messageComposeInputState = MessageComposeInputState.Active(
            messageText = messageComposeInputState.messageText,
            inputFocused = true,
            type = if (newSelfDeletionDuration == SelfDeletionDuration.None) MessageComposeInputType.NewMessage()
            else MessageComposeInputType.SelfDeletingMessage(newSelfDeletionDuration, isEnforced)
        )
    }

    fun getSelfDeletionTime(): SelfDeletionDuration = currentSelfDeletionTimer.toDuration().toSelfDeletionDuration()

    fun shouldShowSelfDeletionOption(): Boolean = with(currentSelfDeletionTimer) {
        // We shouldn't show the self-deleting option if there is a compulsory duration already set on the team settings level
        this !is SelfDeletionTimer.Disabled && !isEnforced
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

fun MessageMention.toUiMention(originalText: String) = UiMention(
    start = this.start,
    length = this.length,
    userId = this.userId,
    handler = originalText.substring(start, start + length)
)
