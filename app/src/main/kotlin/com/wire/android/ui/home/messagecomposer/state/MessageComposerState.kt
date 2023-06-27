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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.message.mention.MessageMention
import kotlin.time.Duration

@Suppress("LongParameterList")
@Composable
fun rememberMessageComposerStateHolder(
    messageComposerViewState: MessageComposerViewState
): MessageComposerStateHolder {
    val context = LocalContext.current

    val mentionStyle = SpanStyle(
        color = MaterialTheme.wireColorScheme.onPrimaryVariant,
        background = MaterialTheme.wireColorScheme.primaryVariant
    )

    val messageCompositionHolder = remember {
        MessageCompositionHolder(
            context = context,
            mentionStyle = mentionStyle
        )
    }

    val additionalOptionStateHolder = remember {
        AdditionalOptionStateHolder()
    }

    return remember(messageComposerViewState) {
        val messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
            messageCompositionHolder = messageCompositionHolder,
            defaultInputType = if(messageComposerViewState.selfDeletionTimer.toDuration() > Duration.ZERO) {
                MessageCompositionInputStateHolder.InputType.SelfDeleting
            } else {
                MessageCompositionInputStateHolder.InputType.Composing
            },
            defaultInputState =  if(messageComposerViewState.selfDeletionTimer.toDuration() > Duration.ZERO) {
                MessageCompositionInputStateHolder.InputType.SelfDeleting
            } else {
                MessageCompositionInputStateHolder.InputType.Composing
            },
        )

        MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            messageCompositionInputStateHolder = messageCompositionInputStateHolder,
            messageCompositionHolder = messageCompositionHolder,
            additionalOptionStateHolder = additionalOptionStateHolder,
        )
    }
}

class MessageComposerStateHolder(
    val messageComposerViewState: MessageComposerViewState,
    val messageCompositionInputStateHolder: MessageCompositionInputStateHolder,
    val messageCompositionHolder: MessageCompositionHolder,
    val additionalOptionStateHolder: AdditionalOptionStateHolder
) {

    val messageComposition = messageCompositionHolder.messageComposition.value

    fun toInActive() {
        messageCompositionInputStateHolder.toInActive()
    }

    fun toActive(showAttachmentOption: Boolean) {
        messageCompositionInputStateHolder.toActive(!showAttachmentOption)
        if (showAttachmentOption) {
            additionalOptionStateHolder.showAdditionalOptionsMenu()
        } else {
            additionalOptionStateHolder.hideAdditionalOptionsMenu()
        }
    }

    fun toEdit(messageId: String, editMessageText: String, mentions: List<MessageMention>) {
        messageCompositionHolder.setEditText(messageId, editMessageText, mentions)
        messageCompositionInputStateHolder.toEdit()
    }

    fun toSelfDeleting() {
        messageCompositionInputStateHolder.toSelfDeleting()
    }

    fun toReply(message: UIMessage.Regular) {
        messageCompositionHolder.setReply(message)
        messageCompositionInputStateHolder.toComposing()
    }

    fun sendMessage() {
//        onSendMessage(messageComposition.toMessageBundle())
        messageCompositionHolder.clear()
    }

}

