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
        inputType = MessageCompositionInputType.SelfDeleting(
            messageCompositionState = messageCompositionHolder.messageComposition,
            onShowEphemeralOptionsMenu = onShowEphemeralOptionsMenu
        )
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

}


fun MessageMention.toUiMention(originalText: String) = UiMention(
    start = this.start,
    length = this.length,
    userId = this.userId,
    handler = originalText.substring(start, start + length)
)
