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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.messagecomposer.UiMention
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.EMPTY
import com.wire.android.util.MENTION_SYMBOL
import com.wire.android.util.NEW_LINE_SYMBOL
import com.wire.android.util.WHITE_SPACE
import com.wire.android.util.ui.toUIText
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration

class MessageCompositionHolder(
    private val context: Context,
    private val requestMentions: (String) -> Unit
) {

    val messageComposition: MutableState<MessageComposition> = mutableStateOf(MessageComposition.DEFAULT)

    fun setReply(message: UIMessage.Regular) {
        val senderId = message.header.userId ?: return

        mapToQuotedContent(message)?.let { quotedContent ->
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
                    messageTextFieldValue = TextFieldValue(""),
                    quotedMessage = quotedMessage
                )
            }
        }
    }

    fun clearReply() {
        messageComposition.update {
            it.copy(quotedMessage = null)
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

    fun setMessageText(messageTextFieldValue: TextFieldValue) {
        updateMentionsIfNeeded(messageTextFieldValue)
        requestMentionSuggestionIfNeeded(messageTextFieldValue)

        messageComposition.update {
            it.copy(
                messageTextFieldValue = messageTextFieldValue
            )
        }
    }

    fun startMention() {
        messageComposition.update { it.copy(messageTextFieldValue = it.mentionSelection()) }
    }

    fun addMention(contact: Contact) {
        val mention = UiMention(
            start = messageComposition.value.messageTextFieldValue.currentMentionStartIndex(),
            length = contact.name.length + 1, // +1 cause there is an "@" before it
            userId = UserId(contact.id, contact.domain),
            handler = String.MENTION_SYMBOL + contact.name
        )

        messageComposition.update { it.copy(messageTextFieldValue = it.insertMentionIntoText(mention)) }
        messageComposition.update { it.copy(selectedMentions = it.selectedMentions.plus(mention).sortedBy { it.start }) }
    }

    private fun updateMentionsIfNeeded(messageText: TextFieldValue) {
        messageComposition.update { it.copy(selectedMentions = it.updateMentions(messageText)) }
    }

    private fun requestMentionSuggestionIfNeeded(text: TextFieldValue) {
        if (text.selection.min != text.selection.max) {
            return
        } else {
            val mentions = messageComposition.value.selectedMentions
            mentions.firstOrNull { text.selection.min in it.start..it.start + it.length }?.let {
                return
            }
        }

        val currentMentionStartIndex = text.currentMentionStartIndex()

        if (currentMentionStartIndex >= 0) {
            // +1 cause need to remove @ symbol at the begin of string
            val textBetweenAtAndSelection = text.text.subSequence(currentMentionStartIndex + 1, text.selection.min)
            if (!textBetweenAtAndSelection.contains(String.WHITE_SPACE)) {

                requestMentions(textBetweenAtAndSelection.toString())
            }
        }
    }

    private fun applyMentionStylesIntoText(text: TextFieldValue): TextFieldValue {
        // For now there is a known issue in Compose
        // https://issuetracker.google.com/issues/199768107
        // It do not allow us to set some custom SpanStyle into "EditableTextView" :(
        // But maybe someday they'll fix it, so we could use it
//        val spanStyles = mentions.map { mention ->
//            AnnotatedString.Range(mentionSpanStyle, mention.start, mention.start + mention.length)
//        }

        //        return text.copy(
        //            annotatedString = AnnotatedString(
        //                text.annotatedString.text,
        //                spanStyles,
        //                text.annotatedString.paragraphStyles
        //            )
        //        )
        return text
    }

    fun setMentionsSearchResult(mentions: List<Contact>) {
        messageComposition.update { it.copy(mentionSearchResult = mentions) }
    }

}

data class MessageComposition(
    val messageTextFieldValue: TextFieldValue = TextFieldValue(""),
    val quotedMessage: UIQuotedMessage.UIQuotedData? = null,
    val selectedMentions: List<UiMention> = emptyList(),
    val mentionSearchResult: List<Contact> = emptyList(),
    val selfDeletionTimer: SelfDeletionTimer
) {
    companion object {
        val DEFAULT = MessageComposition(
            messageTextFieldValue = TextFieldValue(text = ""),
            quotedMessage = null,
            selectedMentions = emptyList(),
            selfDeletionTimer = SelfDeletionTimer.Enabled(Duration.ZERO)
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

    fun insertMentionIntoText(mention: UiMention): TextFieldValue {
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

    fun updateMentions(newMessageText: TextFieldValue): List<UiMention> {
        val result = mutableSetOf<UiMention>()

        selectedMentions.forEach { mention ->
            if (newMessageText.text.length >= mention.start + mention.length) {
                val substringInMentionPlace = newMessageText.text.substring(mention.start, mention.start + mention.length)
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

}

fun MutableState<MessageComposition>.update(block: (MessageComposition) -> MessageComposition) {
    val currentValue = value
    value = block(currentValue)
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
