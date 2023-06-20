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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.messagecomposer.UiMention
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration

@Composable
fun rememberMessageComposerState(
    isFileSharingEnabled: Boolean = true,
    selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(Duration.ZERO),
    interactionAvailability: InteractionAvailability = InteractionAvailability.ENABLED,
    securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE,
    onShowEphemeralOptionsMenu: () -> Unit
): MessageComposerState {
    val context = LocalContext.current

    return remember {
        MessageComposerState(
            context = context,
            isFileSharingEnabled = isFileSharingEnabled,
            selfDeletionTimer = selfDeletionTimer,
            interactionAvailability = interactionAvailability,
            securityClassificationType = securityClassificationType,
            onShowEphemeralOptionsMenu = onShowEphemeralOptionsMenu
        )
    }
}

class MessageComposerState(
    private val context: Context,
    val isFileSharingEnabled: Boolean = true,
    selfDeletionTimer: SelfDeletionTimer = SelfDeletionTimer.Enabled(Duration.ZERO),
    val interactionAvailability: InteractionAvailability = InteractionAvailability.ENABLED,
    val securityClassificationType: SecurityClassificationType = SecurityClassificationType.NONE,
    private val onShowEphemeralOptionsMenu: () -> Unit
) {

    private val messageCompositionHolder = MessageCompositionHolder(context)

    val messageComposition
        get() = messageCompositionHolder.messageComposition.value

    var inputFocused: Boolean by mutableStateOf(false)
        private set

    var inputType: MessageCompositionInputType by mutableStateOf(
        MessageCompositionInputType.Composing(messageCompositionHolder.messageComposition)
    )
        private set

    var inputState: MessageCompositionInputState by mutableStateOf(
        MessageCompositionInputState.INACTIVE
    )
        private set

    var inputSize by mutableStateOf(
        MessageCompositionInputSize.COLLAPSED
    )
        private set

    var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(
        AdditionalOptionSubMenuState.Hidden
    )
        private set

    fun toReply(message: UIMessage.Regular) {
        messageCompositionHolder.setReply(message)

        inputFocused = true
        inputType = MessageCompositionInputType.Composing(
            messageCompositionState = messageCompositionHolder.messageComposition
        )
    }

    fun cancelReply() {
        messageCompositionHolder.clearReply()
    }

    fun toInActive() {
        inputFocused = false
        inputState = MessageCompositionInputState.INACTIVE
    }

    fun toActive(showAttachmentOption: Boolean) {
        inputFocused = !showAttachmentOption
        inputState = MessageCompositionInputState.ACTIVE
        additionalOptionsSubMenuState = if (showAttachmentOption) {
            AdditionalOptionSubMenuState.AttachFile
        } else {
            AdditionalOptionSubMenuState.Hidden
        }
    }

    fun toEdit(editMessageText: String) {
        messageCompositionHolder.setMessageText(TextFieldValue(editMessageText))
        inputFocused = true
        inputType = MessageCompositionInputType.Editing(
            messageCompositionState = messageCompositionHolder.messageComposition,
            messageCompositionSnapShot = messageCompositionHolder.messageComposition.value
        )
    }

    fun toSelfDeleting() {
        inputFocused = true
        inputType = MessageCompositionInputType.SelfDeleting(messageCompositionHolder.messageComposition, onShowEphemeralOptionsMenu)
    }

    fun onMessageTextChanged(messageTextFieldValue: TextFieldValue) {
        messageCompositionHolder.setMessageText(messageTextFieldValue)
    }

    fun onInputFocused() {
        inputType = MessageCompositionInputType.Composing(messageCompositionHolder.messageComposition)
        inputFocused = true
    }

    fun toggleFullScreenInput() {
        inputSize = if (inputSize == MessageCompositionInputSize.COLLAPSED) {
            MessageCompositionInputSize.EXPANDED
        } else {
            MessageCompositionInputSize.COLLAPSED
        }
    }

//    fun startMention() {
//        val beforeSelection = messageCompositionHolder.value.messageTextFieldValue.text
//            .subSequence(0, messageCompositionHolder.value.messageTextFieldValue.selection.min)
//            .run {
//                if (endsWith(String.WHITE_SPACE) || endsWith(String.NEW_LINE_SYMBOL) || this == String.EMPTY) {
//                    this.toString()
//                } else {
//                    StringBuilder(this)
//                        .append(String.WHITE_SPACE)
//                        .toString()
//                }
//            }
//
//        val afterSelection = messageCompositionHolder.value.messageText
//            .subSequence(
//                messageCompositionHolder.value.messageTextFieldValue.selection.max,
//                messageCompositionHolder.value.messageTextFieldValue.text.length
//            )
//
//        val resultText = StringBuilder(beforeSelection)
//            .append(String.MENTION_SYMBOL)
//            .append(afterSelection)
//            .toString()
//
//        val newSelection = TextRange(beforeSelection.length + 1)
//
//        messageCompositionHolder.update { it.copy(messageTextFieldValue = TextFieldValue(resultText, newSelection)) }
//    }
//
//    fun addMention(contact: Contact) {
//        val mention = UiMention(
//            start = messageCompositionHolder.value.messageTextFieldValue.currentMentionStartIndex(),
//            length = contact.name.length + 1, // +1 cause there is an "@" before it
//            userId = UserId(contact.id, contact.domain),
//            handler = String.MENTION_SYMBOL + contact.name
//        )
//
//        insertMentionIntoText(mention)
//
//        messageCompositionHolder.update { it.copy(mentions = it.mentions.plus(mention).sortedBy { it.start }) }
//    }


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

//    private fun insertMentionIntoText(mention: UiMention) {
//        val beforeMentionText = messageCompositionHolder.value.messageTextFieldValue.text
//            .subSequence(0, mention.start)
//        val afterMentionText = messageCompositionHolder.value.messageTextFieldValue.text
//            .subSequence(
//                messageCompositionHolder.value.messageTextFieldValue.selection.max,
//                messageCompositionHolder.value.messageTextFieldValue.text.length
//            )
//        val resultText = StringBuilder()
//            .append(beforeMentionText)
//            .append(mention.handler)
//            .apply {
//                if (!afterMentionText.startsWith(String.WHITE_SPACE)) append(String.WHITE_SPACE)
//            }
//            .append(afterMentionText)
//            .toString()
//
//        // + 1 cause we add space after mention and move selector there
//        val newSelection = TextRange(beforeMentionText.length + mention.handler.length + 1)
//        messageCompositionHolder.update { it.copy(messageTextFieldValue = TextFieldValue(resultText, newSelection)) }
//    }
//
//    private fun updateMentionsIfNeeded(newText: TextFieldValue) {
//        val updatedMentions = mutableSetOf<UiMention>()
//        messageCompositionHolder.value.mentions.forEach { mention ->
//            if (newText.text.length >= mention.start + mention.length) {
//                val substringInMentionPlace = newText.text.substring(mention.start, mention.start + mention.length)
//                if (substringInMentionPlace == mention.handler) {
//                    updatedMentions.add(mention)
//                    return@forEach
//                }
//            }
//
//            val prevMentionEnd = updatedMentions.lastOrNull()?.let { it.start + it.length } ?: 0
//            val newIndexOfMention = newText.text.indexOf(mention.handler, prevMentionEnd)
//            if (newIndexOfMention >= 0) {
//                updatedMentions.add(mention.copy(start = newIndexOfMention))
//            }
//        }
//
//        messageCompositionHolder.update { it.copy(mentions = updatedMentions.toList()) }
//    }

    private fun requestMentionSuggestionIfNeeded(text: TextFieldValue) {
//        if (text.selection.min != text.selection.max) {
//            _mentionQueryFlowState.value = null
//            return
//        } else {
//            mentions.firstOrNull { text.selection.min in it.start..it.start + it.length }?.let {
//                _mentionQueryFlowState.value = null
//                return
//            }
//        }
//
//        val currentMentionStartIndex = text.currentMentionStartIndex()
//
//        if (currentMentionStartIndex >= 0) {
//            // +1 cause need to remove @ symbol at the begin of string
//            val textBetweenAtAndSelection = text.text.subSequence(currentMentionStartIndex + 1, text.selection.min)
//            if (!textBetweenAtAndSelection.contains(String.WHITE_SPACE)) {
//                _mentionQueryFlowState.value = textBetweenAtAndSelection.toString()
//            } else {
//                _mentionQueryFlowState.value = null
//            }
//        } else {
//            _mentionQueryFlowState.value = null
//        }
    }

}


fun MessageMention.toUiMention(originalText: String) = UiMention(
    start = this.start,
    length = this.length,
    userId = this.userId,
    handler = originalText.substring(start, start + length)
)
