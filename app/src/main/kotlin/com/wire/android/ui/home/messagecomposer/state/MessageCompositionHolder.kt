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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.substring
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
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
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.draft.MessageDraft
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Class responsible for orchestrating the state of the message that the user is composing.
 *  A single entry point to update the state of the message.
 */
@Suppress("TooManyFunctions")
class MessageCompositionHolder(
    val messageComposition: MutableState<MessageComposition>,
    var messageTextFieldValue: MutableState<TextFieldValue>,
    val onClearDraft: () -> Unit,
    private val onSaveDraft: (MessageDraft) -> Unit,
    private val onSearchMentionQueryChanged: (String) -> Unit,
    private val onClearMentionSearchResult: () -> Unit,
    private val onTypingEvent: (TypingIndicatorMode) -> Unit,
) {
    private companion object {
        const val RICH_TEXT_MARKDOWN_MULTIPLIER = 2
    }

    fun updateQuote(quotedMessage: UIQuotedMessage.UIQuotedData) {
        messageComposition.update {
            it.copy(
                quotedMessage = quotedMessage,
                quotedMessageId = quotedMessage.messageId,
                editMessageId = null
            )
        }
        onSaveDraft(messageComposition.value.toDraft(messageTextFieldValue.value.text))
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
        onSaveDraft(messageComposition.value.toDraft(messageTextFieldValue.value.text))
    }

    fun clearReply() {
        messageComposition.update {
            it.copy(
                quotedMessage = null,
                quotedMessageId = null
            )
        }
        onSaveDraft(messageComposition.value.toDraft(String.EMPTY))
    }

    suspend fun handleMessageTextUpdates() {
        snapshotFlow { messageTextFieldValue.value.text to messageTextFieldValue.value.selection }
            .distinctUntilChanged()
            .collectLatest { (messageText, selection) ->
                updateTypingEvent(messageText)
                updateMentionsIfNeeded(messageText)
                requestMentionSuggestionIfNeeded(messageText, selection)
                onSaveDraft(messageComposition.value.toDraft(messageText))
            }
    }

    private fun updateTypingEvent(messageText: String) {
        when {
            messageText.isEmpty() -> onTypingEvent(TypingIndicatorMode.STOPPED)
            messageText.isNotEmpty() && messageComposition.value.draftText != messageText -> onTypingEvent(TypingIndicatorMode.STARTED)
        }
    }

    private fun updateMentionsIfNeeded(messageText: String) {
        messageComposition.update { it.copy(selectedMentions = it.getSelectedMentions(messageText)) }
    }

    private fun requestMentionSuggestionIfNeeded(messageText: String, selection: TextRange) {
        if (selection.min != selection.max) {
            onClearMentionSearchResult()
            return
        } else {
            val mentions = messageComposition.value.selectedMentions
            mentions.firstOrNull { selection.min in it.start..it.start + it.length }?.let {
                onClearMentionSearchResult()
                return
            }
        }

        val currentMentionStartIndex = currentMentionStartIndex(messageText, selection)

        if (currentMentionStartIndex >= 0) {
            // +1 cause need to remove @ symbol at the begin of string
            val textBetweenAtAndSelection = messageText.subSequence(
                currentMentionStartIndex + 1,
                selection.min
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

    fun startMention() {
        val beforeSelection = messageTextFieldValue.value.text
            .subSequence(0, messageTextFieldValue.value.selection.min)
            .run {
                if (endsWith(String.WHITE_SPACE) || endsWith(String.NEW_LINE_SYMBOL) || this == String.EMPTY) {
                    this.toString()
                } else {
                    StringBuilder(this)
                        .append(String.WHITE_SPACE)
                        .toString()
                }
            }

        val afterSelection = messageTextFieldValue.value.text
            .subSequence(
                messageTextFieldValue.value.selection.max,
                messageTextFieldValue.value.text.length
            )

        val resultText = StringBuilder(beforeSelection)
            .append(String.MENTION_SYMBOL)
            .append(afterSelection)
            .toString()

        val newSelection = TextRange(beforeSelection.length + 1)
        messageTextFieldValue.value = messageTextFieldValue.value.copy(
            text = messageTextFieldValue.value.text.replaceRange(0, messageTextFieldValue.value.text.length, resultText),
            selection = newSelection
        )
        requestMentionSuggestionIfNeeded(resultText, newSelection)
    }

    fun addMention(contact: Contact) {
        val mention = UIMention(
            start = currentMentionStartIndex(messageTextFieldValue.value.text, messageTextFieldValue.value.selection),
            length = contact.name.length + 1, // +1 cause there is an "@" before it
            userId = UserId(contact.id, contact.domain),
            handler = String.MENTION_SYMBOL + contact.name
        )
        insertMentionIntoText(mention)
        messageComposition.update {
            it.copy(
                selectedMentions = it.selectedMentions.plus(mention).sortedBy { it.start }
            )
        }
    }

    private fun insertMentionIntoText(mention: UIMention) {
        val beforeMentionText = messageTextFieldValue.value.text
            .subSequence(0, mention.start)
        val afterMentionText = messageTextFieldValue.value.text
            .subSequence(
                messageTextFieldValue.value.selection.max,
                messageTextFieldValue.value.text.length
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
        messageTextFieldValue.value = messageTextFieldValue.value.copy(
            text = messageTextFieldValue.value.text.replaceRange(0, messageTextFieldValue.value.text.length, resultText),
            selection = newSelection
        )
        onSaveDraft(messageComposition.value.toDraft(resultText))
    }

    fun setEditText(messageId: String, editMessageText: String, mentions: List<MessageMention>) {
        messageTextFieldValue.value = messageTextFieldValue.value.copy(
            text = editMessageText,
            selection = TextRange(editMessageText.length) // Place cursor at the end of the new text
        )
        messageComposition.update {
            it.copy(
                selectedMentions = mentions.mapNotNull { it.toUiMention(editMessageText) },
                editMessageId = messageId
            )
        }
        onSaveDraft(messageComposition.value.toDraft(editMessageText))
    }

    fun addOrRemoveMessageMarkdown(
        markdown: RichTextMarkdown,
    ) {
        val isHeader = markdown == RichTextMarkdown.Header
        val range = messageTextFieldValue.value.selection
        val selectedText = messageTextFieldValue.value.text.substring(messageTextFieldValue.value.selection)
        val stringBuilder = StringBuilder(messageTextFieldValue.value.text)
        val markdownLength = markdown.value.length
        val markdownLengthComplete =
            if (isHeader) markdownLength else (markdownLength * RICH_TEXT_MARKDOWN_MULTIPLIER)

        val rangeEnd = if (selectedText.contains(markdown.value)) {
            // Remove Markdown
            stringBuilder.replace(
                range.start,
                range.end,
                selectedText.replace(markdown.value, String.EMPTY)
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

        val newMessageText = stringBuilder.toString()
        messageTextFieldValue.value = messageTextFieldValue.value.copy(
            text = messageTextFieldValue.value.text.replaceRange(0, messageTextFieldValue.value.text.length, newMessageText),
            selection = TextRange(selectionStart, selectionEnd)
        )
        onSaveDraft(messageComposition.value.toDraft(newMessageText))
    }

    fun clearMessage() {
        messageTextFieldValue.value = TextFieldValue(String.EMPTY)
        messageComposition.update {
            it.copy(
                quotedMessageId = null,
                quotedMessage = null,
                editMessageId = null
            )
        }
        onSaveDraft(messageComposition.value.toDraft(String.EMPTY))
    }

    fun toMessageBundle(conversationId: ConversationId) =
        messageComposition.value.toMessageBundle(conversationId, messageTextFieldValue.value.text)

    private fun currentMentionStartIndex(messageText: String, selection: TextRange): Int {
        val lastIndexOfAt = messageText.lastIndexOf(String.MENTION_SYMBOL, selection.min - 1)

        return when {
            (lastIndexOfAt <= 0) || (
                    messageText[lastIndexOfAt - 1].toString() in listOf(
                        String.WHITE_SPACE,
                        String.NEW_LINE_SYMBOL
                    )
                    ) -> lastIndexOfAt

            else -> -1
        }
    }
}

enum class RichTextMarkdown(val value: String) {
    Header("# "),
    Bold("**"),
    Italic("_")
}
