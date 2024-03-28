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
package com.wire.android.ui.home.messagecomposer.model

import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.util.EMPTY
import com.wire.android.util.MENTION_SYMBOL
import com.wire.android.util.NEW_LINE_SYMBOL
import com.wire.android.util.WHITE_SPACE
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.draft.MessageDraft

data class MessageComposition(
    val messageTextFieldValue: TextFieldValue = TextFieldValue(""),
    val editMessageId: String? = null,
    val quotedMessage: UIQuotedMessage.UIQuotedData? = null,
    val quotedMessageId: String? = null,
    val selectedMentions: List<UIMention> = emptyList(),
) {
    companion object {
        val DEFAULT = MessageComposition(
            messageTextFieldValue = TextFieldValue(text = ""),
            quotedMessage = null,
            selectedMentions = emptyList()
        )
    }

    val messageText: String
        get() = messageTextFieldValue.text

    fun mentionSelection(): TextFieldValue {
        val beforeSelection = messageTextFieldValue.text
            .subSequence(0, messageTextFieldValue.selection.min)
            .run {
                if (endsWith(String.WHITE_SPACE) || endsWith(String.NEW_LINE_SYMBOL) || this == String.EMPTY) {
                    this.toString()
                } else {
                    StringBuilder(this)
                        .append(String.WHITE_SPACE)
                        .toString()
                }
            }

        val afterSelection = messageTextFieldValue.text
            .subSequence(
                messageTextFieldValue.selection.max,
                messageTextFieldValue.text.length
            )

        val resultText = StringBuilder(beforeSelection)
            .append(String.MENTION_SYMBOL)
            .append(afterSelection)
            .toString()

        val newSelection = TextRange(beforeSelection.length + 1)

        return TextFieldValue(resultText, newSelection)
    }

    fun insertMentionIntoText(mention: UIMention): TextFieldValue {
        val beforeMentionText = messageTextFieldValue.text
            .subSequence(0, mention.start)

        val afterMentionText = messageTextFieldValue.text
            .subSequence(
                messageTextFieldValue.selection.max,
                messageTextFieldValue.text.length
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

    fun getSelectedMentions(newMessageText: TextFieldValue): List<UIMention> {
        val result = mutableSetOf<UIMention>()

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

    fun toMessageBundle(conversationId: ConversationId): ComposableMessageBundle {
        return if (editMessageId != null) {
            ComposableMessageBundle.EditMessageBundle(
                conversationId = conversationId,
                originalMessageId = editMessageId,
                newContent = messageTextFieldValue.text,
                newMentions = selectedMentions
            )
        } else {
            ComposableMessageBundle.SendTextMessageBundle(
                conversationId = conversationId,
                message = messageTextFieldValue.text,
                mentions = selectedMentions,
                quotedMessageId = quotedMessageId
            )
        }
    }
}

fun MutableState<MessageComposition>.update(block: (MessageComposition) -> MessageComposition) {
    val currentValue = value
    value = block(currentValue)
}

fun MessageComposition.toDraft(): MessageDraft {
    return MessageDraft(
        text = messageTextFieldValue.text,
        editMessageId = editMessageId,
        quotedMessageId = quotedMessageId,
        selectedMentionList = selectedMentions.map { it.intoMessageMention() }
    )
}
