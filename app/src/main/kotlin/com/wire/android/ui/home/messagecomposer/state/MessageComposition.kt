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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.messagecomposer.UiMention
import com.wire.android.util.EMPTY
import com.wire.android.util.MENTION_SYMBOL
import com.wire.android.util.NEW_LINE_SYMBOL
import com.wire.android.util.WHITE_SPACE
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration

class MessageCompositionHolder(default: MessageComposition) {

    val messageComposition: MutableState<MessageComposition> = mutableStateOf(default)

}

data class MessageComposition(
    val messageTextFieldValue: TextFieldValue = TextFieldValue(""),
    val quotedMessage: UIQuotedMessage.UIQuotedData? = null,
    val mentions: List<UiMention> = emptyList(),
    val selfDeletionTimer: SelfDeletionTimer
) {
    companion object {
        val DEFAULT = MessageComposition(
            messageTextFieldValue = TextFieldValue(text = ""),
            quotedMessage = null,
            mentions = emptyList(),
            selfDeletionTimer = SelfDeletionTimer.Enabled(Duration.ZERO)
        )
    }

    val messageText: String
        get() = messageTextFieldValue.text


    fun test() {
//        val beforeSelection = messageTextFieldValue.text
//            .subSequence(0, messageTextFieldValue.selection.min)
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
//        val afterSelection = messageText
//            .subSequence(
//                messageTextFieldValue.selection.max,
//                messageTextFieldValue.text.length
//            )
//
//        val resultText = StringBuilder(beforeSelection)
//            .append(String.MENTION_SYMBOL)
//            .append(afterSelection)
//            .toString()
//
//        return TextRange(beforeSelection.length + 1)
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
