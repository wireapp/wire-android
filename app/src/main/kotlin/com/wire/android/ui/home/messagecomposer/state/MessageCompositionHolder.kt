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
 */
package com.wire.android.ui.home.messagecomposer.state

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.UiMention
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.EMPTY
import com.wire.android.util.MENTION_SYMBOL
import com.wire.android.util.NEW_LINE_SYMBOL
import com.wire.android.util.WHITE_SPACE
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId

/**
 * Class responsible for orchestrating the state of the message that the user is composing.
 *  A single entry point to update the state of the message.
 */
class MessageCompositionHolder(
    private val context: Context,
    // we provide reference MutableState outside of the MessageComposition because TextFiedValue
    // is created using remeberSaveable inside rememberMessageComposerStateHolder
    val messageText: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue("")),
    editMessageId: String? = null,
    quotedMessage: UIQuotedMessage.UIQuotedData? = null,
    quotedMessageId: String? = null,
    selectedMentions: List<UiMention> = emptyList()
) {
    var editMessageId by mutableStateOf(editMessageId)
        private set

    var quotedMessage by mutableStateOf(quotedMessage)
        private set
    var quotedMessageId by mutableStateOf(quotedMessageId)
        private set
    var selectedMentions by mutableStateOf(selectedMentions)
        private set

    private companion object {
        const val RICH_TEXT_MARKDOWN_MULTIPLIER = 2
    }

    fun setReply(message: UIMessage.Regular) {
        val senderId = message.header.userId ?: return

        mapToQuotedContent(message)?.let { quotedContent ->
            messageText.value = TextFieldValue("")
            quotedMessage = UIQuotedMessage.UIQuotedData(
                messageId = message.header.messageId,
                senderId = senderId,
                senderName = message.header.username,
                originalMessageDateDescription = "".toUIText(),
                editedTimeDescription = "".toUIText(),
                quotedContent = quotedContent
            )
            quotedMessageId = message.header.messageId
        }
    }

    private fun mapToQuotedContent(message: UIMessage.Regular) =
        when (val messageContent = message.messageContent) {
            is UIMessageContent.AssetMessage -> UIQuotedMessage.UIQuotedData.GenericAsset(
                assetName = messageContent.assetName,
                assetMimeType = messageContent.assetExtension
            )

            is UIMessageContent.RestrictedAsset -> UIQuotedMessage.UIQuotedData.GenericAsset(
                assetName = messageContent.assetName,
                assetMimeType = messageContent.mimeType
            )

            is UIMessageContent.TextMessage -> UIQuotedMessage.UIQuotedData.Text(
                value = messageContent.messageBody.message.asString(context.resources)
            )

            is UIMessageContent.AudioAssetMessage -> UIQuotedMessage.UIQuotedData.AudioMessage

            is UIMessageContent.ImageMessage -> messageContent.asset?.let {
                UIQuotedMessage.UIQuotedData.DisplayableImage(
                    displayable = messageContent.asset
                )
            }

            else -> {
                appLogger.w("Attempting to reply to an unsupported message type of content = $messageContent")
                null
            }
        }

    fun clearReply() {
        quotedMessage = null
        quotedMessageId = null
    }

    fun setMessageText(
        messageTextFieldValue: TextFieldValue,
        onSearchMentionQueryChanged: (String) -> Unit,
        onClearMentionSearchResult: () -> Unit
    ) {
        updateMentionsIfNeeded(messageTextFieldValue)
        requestMentionSuggestionIfNeeded(
            messageText = messageTextFieldValue,
            onSearchMentionQueryChanged = onSearchMentionQueryChanged,
            onClearMentionSearchResult = onClearMentionSearchResult
        )

        messageText.value = messageTextFieldValue
    }

    private fun updateMentionsIfNeeded(messageText: TextFieldValue) {
        selectedMentions = getSelectedMentions(messageText)
    }

    private fun requestMentionSuggestionIfNeeded(
        messageText: TextFieldValue,
        onSearchMentionQueryChanged: (String) -> Unit,
        onClearMentionSearchResult: () -> Unit
    ) {
        if (messageText.selection.min != messageText.selection.max) {
            onClearMentionSearchResult()
            return
        } else {
            val mentions = selectedMentions
            mentions.firstOrNull { messageText.selection.min in it.start..it.start + it.length }?.let {
                onClearMentionSearchResult()
                return
            }
        }

        val currentMentionStartIndex = messageText.currentMentionStartIndex()

        if (currentMentionStartIndex >= 0) {
            // +1 cause need to remove @ symbol at the begin of string
            val textBetweenAtAndSelection = messageText.text.subSequence(
                currentMentionStartIndex + 1,
                messageText.selection.min
            )
            if (!textBetweenAtAndSelection.contains(String.WHITE_SPACE)) {
                onSearchMentionQueryChanged(textBetweenAtAndSelection.toString())
            } else {
                onClearMentionSearchResult()
            }
        } else {
            onClearMentionSearchResult()
        }
    }

    fun startMention(
        onSearchMentionQueryChanged: (String) -> Unit,
        onClearMentionSearchResult: () -> Unit
    ) {
        setMessageText(mentionSelection(), onSearchMentionQueryChanged, onClearMentionSearchResult)
    }

    fun addMention(contact: Contact) {
        val mention = UiMention(
            start = messageText.value.currentMentionStartIndex(),
            length = contact.name.length + 1, // +1 cause there is an "@" before it
            userId = UserId(contact.id, contact.domain),
            handler = String.MENTION_SYMBOL + contact.name
        )

        messageText.value = insertMentionIntoText(mention)
        selectedMentions = selectedMentions.plus(mention).sortedBy { it.start }
    }

    fun setEditText(messageId: String, editMessageText: String, mentions: List<MessageMention>) {
        messageText.value = (TextFieldValue(
            text = editMessageText,
            selection = TextRange(editMessageText.length)
        ))
        selectedMentions = mentions.map { it.toUiMention(editMessageText) }
        editMessageId = messageId
    }

    fun addOrRemoveMessageMarkdown(
        markdown: RichTextMarkdown,
    ) {
        val originalValue = messageText.value

        val isBold = markdown == RichTextMarkdown.Bold

        val range = originalValue.selection
        val selectedText = originalValue.getSelectedText()
        val stringBuilder = StringBuilder(originalValue.annotatedString)
        val markdownLength = markdown.value.length
        val markdownLengthComplete =
            if (isBold) markdownLength else (markdownLength * RICH_TEXT_MARKDOWN_MULTIPLIER)

        val rangeEnd = if (selectedText.contains(markdown.value)) {
            // Remove Markdown
            stringBuilder.replace(
                range.start,
                range.end,
                selectedText.toString().replace(markdown.value, String.EMPTY)
            )

            range.end - markdownLengthComplete
        } else {
            // Add Markdown
            stringBuilder.insert(range.start, markdown.value)
            if (isBold.not()) stringBuilder.insert(range.end + markdownLength, markdown.value)

            range.end + markdownLengthComplete
        }

        val (selectionStart, selectionEnd) = if (range.start == range.end) {
            if (isBold) Pair(rangeEnd, rangeEnd)
            else {
                val middleMarkdownRange = rangeEnd - markdownLength
                Pair(middleMarkdownRange, middleMarkdownRange)
            }
        } else {
            Pair(range.start, rangeEnd)
        }


        messageText.value = TextFieldValue(
            text = stringBuilder.toString(),
            selection = TextRange(
                start = selectionStart,
                end = selectionEnd
            )
        )
    }

    fun clearMessage() {
        messageText.value = TextFieldValue("")
        editMessageId = null
    }

    fun mentionSelection(): TextFieldValue {
        val beforeSelection = messageText.value.text
            .subSequence(0, messageText.value.selection.min)
            .run {
                if (endsWith(String.WHITE_SPACE) || endsWith(String.NEW_LINE_SYMBOL) || this == String.EMPTY) {
                    this.toString()
                } else {
                    StringBuilder(this)
                        .append(String.WHITE_SPACE)
                        .toString()
                }
            }

        val afterSelection = messageText.value.text
            .subSequence(
                messageText.value.selection.max,
                messageText.value.text.length
            )

        val resultText = StringBuilder(beforeSelection)
            .append(String.MENTION_SYMBOL)
            .append(afterSelection)
            .toString()

        val newSelection = TextRange(beforeSelection.length + 1)

        return TextFieldValue(resultText, newSelection)
    }

    fun insertMentionIntoText(mention: UiMention): TextFieldValue {
        val beforeMentionText = messageText.value.text
            .subSequence(0, mention.start)

        val afterMentionText = messageText.value.text
            .subSequence(
                messageText.value.selection.max,
                messageText.value.text.length
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

        return TextFieldValue(resultText, newSelection)
    }

    fun getSelectedMentions(newMessageText: TextFieldValue): List<UiMention> {
        val result = mutableSetOf<UiMention>()

        selectedMentions.forEach { mention ->
            if (newMessageText.text.length >= mention.start + mention.length) {
                val substringInMentionPlace = newMessageText.text.substring(
                    mention.start,
                    mention.start + mention.length
                )
                if (substringInMentionPlace == mention.handler) {
                    result.add(mention)
                    return@forEach
                }
            }

            val prevMentionEnd = result.lastOrNull()?.let { it.start + it.length } ?: 0
            val newIndexOfMention = newMessageText.text.indexOf(mention.handler, prevMentionEnd)
            if (newIndexOfMention >= 0) {
                result.add(mention.copy(start = newIndexOfMention))
            }
        }

        return result.toList()
    }

    fun toMessageBundle(): ComposableMessageBundle {
        val messageId = editMessageId

        return if (messageId != null) {
            ComposableMessageBundle.EditMessageBundle(
                originalMessageId = messageId,
                newContent = messageText.value.text,
                newMentions = selectedMentions
            )
        } else {
            ComposableMessageBundle.SendTextMessageBundle(
                message = messageText.value.text,
                mentions = selectedMentions,
                quotedMessageId = quotedMessageId
            )
        }
    }
}

private fun MessageMention.toUiMention(originalText: String) = UiMention(
    start = start,
    length = length,
    userId = userId,
    handler = originalText.substring(start, start + length)
)

private fun TextFieldValue.currentMentionStartIndex(): Int {
    val lastIndexOfAt = text.lastIndexOf(String.MENTION_SYMBOL, selection.min - 1)

    return when {
        (lastIndexOfAt <= 0) || (
                text[lastIndexOfAt - 1].toString() in listOf(
                    String.WHITE_SPACE,
                    String.NEW_LINE_SYMBOL
                )
                ) -> lastIndexOfAt

        else -> -1
    }
}

interface MessageBundle

sealed class ComposableMessageBundle : MessageBundle {
    data class EditMessageBundle(
        val originalMessageId: String,
        val newContent: String,
        val newMentions: List<UiMention>
    ) : ComposableMessageBundle()

    data class SendTextMessageBundle(
        val message: String,
        val mentions: List<UiMention>,
        val quotedMessageId: String? = null
    ) : ComposableMessageBundle()

    data class AttachmentPickedBundle(
        val attachmentUri: UriAsset
    ) : ComposableMessageBundle()
}

object Ping : MessageBundle

enum class RichTextMarkdown(val value: String) {
    Header("# "),
    Bold("**"),
    Italic("_")
}
