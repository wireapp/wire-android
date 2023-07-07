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
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer

@Suppress("LongParameterList")
@Composable
fun rememberMessageComposerStateHolder(
    messageComposerViewState: MutableState<MessageComposerViewState>,
    modalBottomSheetState: WireModalSheetState
): MessageComposerStateHolder {
    val context = LocalContext.current

    val messageCompositionHolder = remember {
        MessageCompositionHolder(
            context = context
        )
    }

    // we derive the selfDeletionTimer from the messageCompositionHolder as a state in order to "observe" the changes to it
    // which are made "externally" and not inside the MessageComposer.
    val selfDeletionTimer = remember {
        derivedStateOf {
            messageComposerViewState.value.selfDeletionTimer
        }
    }

    val messageCompositionInputStateHolder = remember {
        MessageCompositionInputStateHolder(
            messageComposition = messageCompositionHolder.messageComposition,
            selfDeletionTimer = selfDeletionTimer,
        )
    }

    return remember {
        MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            modalBottomSheetState = modalBottomSheetState,
            messageCompositionInputStateHolder = messageCompositionInputStateHolder,
            messageCompositionHolder = messageCompositionHolder,
            additionalOptionStateHolder = AdditionalOptionStateHolder()
        )
    }
}

/**
 * Class holding the whole UI state for the MessageComposer, this is the class that is used by the out-side world to give the control
 * of the state to the parent Composables
 */
class MessageComposerStateHolder(
    val messageComposerViewState: MutableState<MessageComposerViewState>,
    val messageCompositionInputStateHolder: MessageCompositionInputStateHolder,
    val messageCompositionHolder: MessageCompositionHolder,
    val additionalOptionStateHolder: AdditionalOptionStateHolder,
    val modalBottomSheetState: WireModalSheetState
) {
    val messageComposition = messageCompositionHolder.messageComposition

    val isTransitionToKeyboardOnGoing
        @Composable get() = additionalOptionStateHolder.additionalOptionsSubMenuState == AdditionalOptionSubMenuState.Hidden &&
                !KeyboardHelper.isKeyboardVisible() &&
                messageCompositionInputStateHolder.inputFocused
    val additionalOptionSubMenuVisible
        @Composable get() = additionalOptionStateHolder.additionalOptionsSubMenuState != AdditionalOptionSubMenuState.Hidden &&
                !KeyboardHelper.isKeyboardVisible()

    private var isKeyboardVisible = false

    val isSelfDeletingSettingEnabled = messageComposerViewState.value.selfDeletionTimer !is SelfDeletionTimer.Disabled &&
            messageComposerViewState.value.selfDeletionTimer !is SelfDeletionTimer.Enforced

    fun toInActive() {
        messageCompositionInputStateHolder.toInActive()
        messageCompositionInputStateHolder.clearFocus()
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

    fun toAudioRecording() {
        messageCompositionInputStateHolder.hide()
        additionalOptionStateHolder.toAudioRecording()
    }

    fun toCloseAudioRecording() {
        messageCompositionInputStateHolder.show()
        additionalOptionStateHolder.hideAudioRecording()
    }

    fun onKeyboardVisibilityChanged(isVisible: Boolean) {
        val isKeyboardClosed = isKeyboardVisible && !isVisible

        if (isKeyboardClosed) {
            if (!modalBottomSheetState.isVisible && additionalOptionStateHolder.additionalOptionsSubMenuState == AdditionalOptionSubMenuState.Hidden) {
                messageCompositionInputStateHolder.toInActive()
            } else {
                messageCompositionInputStateHolder.clearFocus()
            }
        }

        isKeyboardVisible = isVisible
    }

    fun showAdditionalOptionsMenu() {
        additionalOptionStateHolder.showAdditionalOptionsMenu()
        messageCompositionInputStateHolder.clearFocus()
    }

    fun onMessageSend() {
        messageCompositionHolder.clearMessage()
    }
}
