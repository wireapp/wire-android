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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wire.android.ui.common.KeyboardHelper
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.message.mention.MessageMention

@Suppress("LongParameterList")
@Composable
fun rememberMessageComposerStateHolder(
    messageComposerViewState: MutableState<MessageComposerViewState>
): MessageComposerStateHolder {
    val context = LocalContext.current

    val selfDeletionTimer = remember {
        derivedStateOf {
            messageComposerViewState.value.selfDeletionTimer
        }
    }

    return remember {
        val messageCompositionHolder = MessageCompositionHolder(
            context = context,
            selfDeletionTimer = selfDeletionTimer
        )

        MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
                messageComposition = messageCompositionHolder.messageComposition,
                selfDeletionTimer = selfDeletionTimer,
            ),
            messageCompositionHolder = messageCompositionHolder,
            additionalOptionStateHolder = AdditionalOptionStateHolder(),
        )
    }
}

class MessageComposerStateHolder(
    val messageComposerViewState: MutableState<MessageComposerViewState>,
    val messageCompositionInputStateHolder: MessageCompositionInputStateHolder,
    val messageCompositionHolder: MessageCompositionHolder,
    val additionalOptionStateHolder: AdditionalOptionStateHolder
) {
    val messageComposition = messageCompositionHolder.messageComposition

    val isTransitionToKeyboardOnGoing
        @Composable get() = additionalOptionStateHolder.additionalOptionsSubMenuState == AdditionalOptionSubMenuState.Hidden
                && !KeyboardHelper.isKeyboardVisible()
                && messageCompositionInputStateHolder.inputFocused
    val additionalOptionSubMenuVisible
        @Composable get() = additionalOptionStateHolder.additionalOptionsSubMenuState == AdditionalOptionSubMenuState.AttachFile &&
                !KeyboardHelper.isKeyboardVisible()

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

    fun onInputFocusedChanged(onFocused: Boolean) {
        if (onFocused) {
            additionalOptionStateHolder.hideAdditionalOptionsMenu()
            messageCompositionInputStateHolder.requestFocus()
        } else {
            messageCompositionInputStateHolder.clearFocus()
        }
    }

    fun onKeyboardClosed() {
        if (messageCompositionInputStateHolder.inputFocused) {
            toInActive()
        }
    }

    fun showAdditionalOptionsMenu() {
        additionalOptionStateHolder.showAdditionalOptionsMenu()
        messageCompositionInputStateHolder.clearFocus()
    }

    fun sendMessage() {
//        onSendMessage(messageComposition.toMessageBundle())

        messageCompositionHolder.clear()
    }

}

