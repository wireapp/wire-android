/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import android.location.Location
import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.conversations.model.mapToQuotedContent
import com.wire.android.ui.home.conversations.model.toUiMention
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.model.toDraft
import com.wire.android.ui.home.messagecomposer.model.update
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.EMPTY
import com.wire.android.util.MENTION_SYMBOL
import com.wire.android.util.NEW_LINE_SYMBOL
import com.wire.android.util.WHITE_SPACE
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.message.draft.MessageDraft
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId

/**
 * Class responsible for orchestrating the state of the message that the user is composing.
 *  A single entry point to update the state of the message.
 */
class MessageCompositionHolder(
    val messageComposition: MutableState<MessageComposition>,
    private val onSaveDraft: (MessageDraft) -> Unit,
) {
    private companion object {
        const val RICH_TEXT_MARKDOWN_MULTIPLIER = 2
    }

    fun setReply(message: UIMessage.Regular) {
        val senderId = message.header.userId ?: return

        message.mapToQuotedContent()?.let { quotedContent ->
            val quotedMessage = UIQuotedMessage.UIQuotedData(
                messageId = message.header.messageId,
                senderId = senderId,
                senderName = message.header.username,
                originalMessageDateDescription = "".toUIText(),
                editedTimeDescription = "".toUIText(),
                quotedContent = quotedContent
            )

            messageComposition.update {
                it.copy(
                    quotedMessage = quotedMessage,
                    quotedMessageId = message.header.messageId,
                    editMessageId = null
                )
            }
        }
        onSaveDraft(messageComposition.value.toDraft())
    }

    fun clearReply() {
        messageComposition.update {
            it.copy(
                quotedMessage = null,
                quotedMessageId = null
            )
        }
        onSaveDraft(messageComposition.value.toDraft())
    }

    fun setMessageText(
        messageTextFieldValue: TextFieldValue,
        onSearchMentionQueryChanged: (String) -> Unit,
        onClearMentionSearchResult: () -> Unit,
        onTypingEvent: (TypingIndicatorMode) -> Unit,
    ) {
        updateTypingEvent(messageTextFieldValue, onTypingEvent)
        updateMentionsIfNeeded(messageTextFieldValue)
        requestMentionSuggestionIfNeeded(
            messageText = messageTextFieldValue,
            onSearchMentionQueryChanged = onSearchMentionQueryChanged,
            onClearMentionSearchResult = onClearMentionSearchResult
        )

        messageComposition.update {
            it.copy(messageTextFieldValue = messageTextFieldValue)
        }
        onSaveDraft(messageComposition.value.toDraft())
    }

    private fun updateTypingEvent(messageTextFieldValue: TextFieldValue, onTypingEvent: (TypingIndicatorMode) -> Unit) {
        when {
            messageTextFieldValue.text.isEmpty() -> onTypingEvent(TypingIndicatorMode.STOPPED)
            messageTextFieldValue.text.isNotEmpty() && messageComposition.value.messageText != messageTextFieldValue.text ->
                onTypingEvent(TypingIndicatorMode.STARTED)
        }
    }

    private fun updateMentionsIfNeeded(messageText: TextFieldValue) {
        messageComposition.update { it.copy(selectedMentions = it.getSelectedMentions(messageText)) }
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
            val mentions = messageComposition.value.selectedMentions
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
        onClearMentionSearchResult: () -> Unit,
        onTypingEvent: (TypingIndicatorMode) -> Unit
    ) {
        setMessageText(messageComposition.value.mentionSelection(), onSearchMentionQueryChanged, onClearMentionSearchResult, onTypingEvent)
    }

    fun addMention(contact: Contact) {
        val mention = UIMention(
            start = messageComposition.value.messageTextFieldValue.currentMentionStartIndex(),
            length = contact.name.length + 1, // +1 cause there is an "@" before it
            userId = UserId(contact.id, contact.domain),
            handler = String.MENTION_SYMBOL + contact.name
        )

        messageComposition.update {
            it.copy(
                messageTextFieldValue = it.insertMentionIntoText(mention),
                selectedMentions = it.selectedMentions.plus(mention).sortedBy { it.start }
            )
        }
        onSaveDraft(messageComposition.value.toDraft())
    }

    fun setEditText(messageId: String, editMessageText: String, mentions: List<MessageMention>) {
        messageComposition.update {
            it.copy(
                messageTextFieldValue = (TextFieldValue(
                    text = editMessageText,
                    selection = TextRange(editMessageText.length)
                )),
                selectedMentions = mentions.mapNotNull { it.toUiMention(editMessageText) },
                editMessageId = messageId
            )
        }
        onSaveDraft(messageComposition.value.toDraft())
    }

    fun addOrRemoveMessageMarkdown(
        markdown: RichTextMarkdown,
    ) {
        val originalValue = messageComposition.value.messageTextFieldValue

        val isHeader = markdown == RichTextMarkdown.Header

        val range = originalValue.selection
        val selectedText = originalValue.getSelectedText()
        val stringBuilder = StringBuilder(originalValue.annotatedString)
        val markdownLength = markdown.value.length
        val markdownLengthComplete =
            if (isHeader) markdownLength else (markdownLength * RICH_TEXT_MARKDOWN_MULTIPLIER)

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
            if (isHeader.not()) stringBuilder.insert(range.end + markdownLength, markdown.value)

            range.end + markdownLengthComplete
        }

        val (selectionStart, selectionEnd) = if (range.start == range.end) {
            if (isHeader) Pair(rangeEnd, rangeEnd)
            else {
                val middleMarkdownRange = rangeEnd - markdownLength
                Pair(middleMarkdownRange, middleMarkdownRange)
            }
        } else {
            Pair(range.start, rangeEnd)
        }

        messageComposition.update {
            it.copy(
                messageTextFieldValue = TextFieldValue(
                    text = stringBuilder.toString(),
                    selection = TextRange(
                        start = selectionStart,
                        end = selectionEnd
                    )
                )
            )
        }
        onSaveDraft(messageComposition.value.toDraft())
    }

    fun clearMessage() {
        messageComposition.update {
            it.copy(
                messageTextFieldValue = TextFieldValue(""),
                quotedMessageId = null,
                quotedMessage = null,
                editMessageId = null
            )
        }
        onSaveDraft(messageComposition.value.toDraft())
    }

    fun toMessageBundle() = messageComposition.value.toMessageBundle()
}

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
        val newMentions: List<UIMention>
    ) : ComposableMessageBundle()

    data class SendTextMessageBundle(
        val message: String,
        val mentions: List<UIMention>,
        val quotedMessageId: String? = null
    ) : ComposableMessageBundle()

    data class AttachmentPickedBundle(
        val attachmentUri: UriAsset
    ) : ComposableMessageBundle()

    data class AudioMessageBundle(
        val attachmentUri: UriAsset
    ) : ComposableMessageBundle()

    data class LocationBundle(
        val locationName: String,
        val location: Location,
        val zoom: Int = 20
    ) : ComposableMessageBundle()
}

object Ping : MessageBundle

enum class RichTextMarkdown(val value: String) {
    Header("# "),
    Bold("**"),
    Italic("_")
}
