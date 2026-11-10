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

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.substring
import com.wire.android.appLogger
import com.wire.android.model.Contact
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversations.model.mapToQuotedContent
import com.wire.android.ui.home.conversations.model.toUiMention
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.model.toDraft
import com.wire.android.ui.home.messagecomposer.model.update
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
    var messageTextState: TextFieldState,
    val onClearDraft: () -> Unit,
    private val onSaveDraft: (MessageDraft) -> Unit,
    private val onMessageTextUpdate: (MessageDraft) -> Unit,
    private val onSearchMentionQueryChanged: (String) -> Unit,
    private val onClearMentionSearchResult: () -> Unit,
    private val onTypingEvent: (TypingIndicatorMode) -> Unit,
) {
    private var trackedMentionText = messageTextState.text.toString()
    val mentionInputTransformation = InputTransformation { updateMentions(this) }

    /** Programmatic edits use the same range tracking as keyboard, paste, and accessibility input. */
    fun editText(block: TextFieldBuffer.() -> Unit) {
        messageTextState.edit {
            block()
            updateMentions(this)
        }
    }

    fun updateQuote(quotedMessage: UIQuotedMessage.UIQuotedData) {
        messageComposition.update {
            it.copy(
                quotedMessage = quotedMessage,
                quotedMessageId = quotedMessage.messageId,
                editMessageId = null
            )
        }
        onSaveDraft(currentDraft())
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
                quotedContent = quotedContent,
                senderAccent = message.header.accent
            )

            messageComposition.update {
                it.copy(
                    quotedMessage = quotedMessage,
                    quotedMessageId = message.header.messageId,
                    editMessageId = null
                )
            }
        }
        onSaveDraft(currentDraft())
    }

    fun clearReply() {
        messageComposition.update {
            it.copy(
                quotedMessage = null,
                quotedMessageId = null
            )
        }
        onSaveDraft(currentDraft(String.EMPTY))
    }

    suspend fun handleMessageTextUpdates() {
        snapshotFlow { messageTextState.text to messageTextState.selection }
            .distinctUntilChanged()
            .collectLatest { (messageText, selection) ->
                discardUntrackedMentions(messageText.toString())
                updateTypingEvent(messageText.toString())
                requestMentionSuggestionIfNeeded(messageText.toString(), selection)
                onMessageTextUpdate(currentDraft())
            }
    }

    private fun updateMentions(buffer: TextFieldBuffer) {
        discardUntrackedMentions(buffer.originalText.toString())
        messageComposition.update { it.copy(selectedMentions = it.getSelectedMentions(buffer)) }
        trackedMentionText = buffer.asCharSequence().toString()
    }

    private fun discardUntrackedMentions(text: String) {
        if (text != trackedMentionText) {
            // Native undo/redo bypasses InputTransformation. Without an edit range, identity is unknown.
            messageComposition.update { it.copy(selectedMentions = emptyList()) }
            trackedMentionText = text
        }
    }

    private fun currentDraft(text: String = messageTextState.text.toString()): MessageDraft {
        discardUntrackedMentions(messageTextState.text.toString())
        return messageComposition.value.toDraft(text)
    }

    /** Accepts the mention metadata supplied by a trusted draft hydration. */
    fun restoreComposition(composition: MessageComposition) {
        messageComposition.value = composition
        if (composition.draftText.isNotBlank() || composition.editMessageId != null) {
            messageTextState.setTextAndPlaceCursorAtEnd(composition.draftText)
        }
        trackedMentionText = messageTextState.text.toString()
    }

    private fun updateTypingEvent(messageText: String) {
        when {
            messageText.isEmpty() -> onTypingEvent(TypingIndicatorMode.STOPPED)
            messageText.isNotEmpty() && messageComposition.value.draftText != messageText -> onTypingEvent(TypingIndicatorMode.STARTED)
        }
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
        val originalSelection = messageTextState.selection
        val beforeSelection = messageTextState.text
            .subSequence(0, messageTextState.selection.min)
            .run {
                if (endsWith(String.WHITE_SPACE) || endsWith(String.NEW_LINE_SYMBOL) || this == String.EMPTY) {
                    this.toString()
                } else {
                    StringBuilder(this)
                        .append(String.WHITE_SPACE)
                        .toString()
                }
            }

        val newSelection = TextRange(beforeSelection.length + 1)
        editText {
            replace(
                originalSelection.min,
                originalSelection.max,
                beforeSelection.substring(originalSelection.min) + String.MENTION_SYMBOL
            )
            selection = newSelection
        }
        requestMentionSuggestionIfNeeded(messageTextState.text.toString(), newSelection)
    }

    fun addMention(contact: Contact) = addMention(contact.name, UserId(contact.id, contact.domain))

    fun addEveryoneMention() = addMention("all", null)

    private fun addMention(name: String, userId: UserId?) {
        val mention = UIMention(
            start = currentMentionStartIndex(messageTextState.text.toString(), messageTextState.selection),
            length = name.length + 1, // +1 cause there is an "@" before it
            userId = userId,
            handler = String.MENTION_SYMBOL + name
        )
        if (mention.start < 0 || mention.length < 2) {
            appLogger.e("MessageCompositionHolder: Failure to add mention")
            return
        }
        insertMentionIntoText(mention)
        messageComposition.update {
            it.copy(
                selectedMentions = it.selectedMentions.plus(mention).sortedBy { it.start }
            )
        }
        onSaveDraft(currentDraft())
    }

    private fun insertMentionIntoText(mention: UIMention) {
        val replaceEnd = messageTextState.selection.max
        val afterMentionText = messageTextState.text
            .subSequence(
                messageTextState.selection.max,
                messageTextState.text.length
            )
        val replacement = StringBuilder()
            .append(mention.handler)
            .apply {
                if (!afterMentionText.startsWith(String.WHITE_SPACE)) append(String.WHITE_SPACE)
            }
            .toString()

        // + 1 cause we add space after mention and move selector there
        val newSelection = TextRange(mention.start + mention.handler.length + 1)
        editText {
            replace(mention.start, replaceEnd, replacement)
            selection = newSelection
        }
    }

    fun setEditText(messageId: String, editMessageText: String, mentions: List<MessageMention>, isMultipart: Boolean) {
        messageTextState.setTextAndPlaceCursorAtEnd(editMessageText)
        trackedMentionText = editMessageText
        messageComposition.update {
            it.copy(
                selectedMentions = mentions.mapNotNull { mention -> mention.toUiMention(editMessageText) },
                editMessageId = messageId,
                quotedMessage = null,
                quotedMessageId = null,
                isMultipart = isMultipart,
            )
        }
        onSaveDraft(currentDraft(editMessageText))
    }

    fun setMentions(editMessageText: String, mentions: List<MessageMention>) {
        trackedMentionText = messageTextState.text.toString()
        messageComposition.update {
            it.copy(selectedMentions = mentions.mapNotNull { mention -> mention.toUiMention(editMessageText) })
        }
        onSaveDraft(currentDraft(editMessageText))
    }

    fun addOrRemoveMessageMarkdown(
        markdown: RichTextMarkdown,
    ) {
        val isHeader = markdown == RichTextMarkdown.Header
        val range = TextRange(messageTextState.selection.min, messageTextState.selection.max)
        val selectedText = messageTextState.text.substring(messageTextState.selection)
        val markdownLength = markdown.value.length
        val removing = selectedText.contains(markdown.value)
        val replacement = selectedText.replace(markdown.value, String.EMPTY)
        val addedLength = if (isHeader) markdownLength else markdownLength * 2
        val rangeEnd = if (removing) range.start + replacement.length else range.end + addedLength
        val selectionStart = if (range.collapsed) {
            if (isHeader || removing) rangeEnd else rangeEnd - markdownLength
        } else {
            range.start
        }
        editText {
            if (removing) {
                // Delete only formatting characters so untouched mention ranges retain their targets.
                Regex(Regex.escape(markdown.value)).findAll(selectedText).toList().asReversed().forEach {
                    replace(range.start + it.range.first, range.start + it.range.last + 1, String.EMPTY)
                }
            } else {
                if (!isHeader) replace(range.end, range.end, markdown.value)
                replace(range.start, range.start, markdown.value)
            }
            selection = TextRange(selectionStart, if (range.collapsed) selectionStart else rangeEnd)
        }
        onSaveDraft(currentDraft())
    }

    fun clearMessage() {
        messageTextState.clearText()
        messageComposition.update {
            it.copy(
                quotedMessageId = null,
                quotedMessage = null,
                editMessageId = null,
                selectedMentions = emptyList()
            )
        }
        onSaveDraft(currentDraft(String.EMPTY))
    }

    fun toMessageBundle(conversationId: ConversationId, attachments: List<AttachmentDraftUi>): ComposableMessageBundle {
        discardUntrackedMentions(messageTextState.text.toString())
        return messageComposition.value.toMessageBundle(conversationId, messageTextState.text.toString(), attachments)
    }

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
