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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration


class MessageCompositionInputStateHolder(
    selfDeletionTimer: SelfDeletionTimer,
    val securityClassificationType: SecurityClassificationType,
    private val messageCompositionHolder: MessageCompositionHolder,
    private val onShowEphemeralOptionsMenu: () -> Unit
) {
    val messageComposition: MessageComposition
        get() = messageCompositionHolder.messageComposition.value

    var inputFocused: Boolean by mutableStateOf(false)
        private set

    var inputType: MessageCompositionInputType by mutableStateOf(
        if (selfDeletionTimer.toDuration() > Duration.ZERO) {
            MessageCompositionInputType.SelfDeleting(
                messageCompositionState = messageCompositionHolder.messageComposition,
                onShowEphemeralOptionsMenu = onShowEphemeralOptionsMenu
            )
        } else {
            MessageCompositionInputType.Composing(messageCompositionHolder.messageComposition)
        }
    )
        private set

    var inputState: MessageCompositionInputState by mutableStateOf(
        if (selfDeletionTimer.toDuration() > Duration.ZERO) {
            MessageCompositionInputState.ACTIVE
        } else {
            MessageCompositionInputState.INACTIVE
        }
    )
        private set

    var inputSize by mutableStateOf(
        MessageCompositionInputSize.COLLAPSED
    )
        private set


    fun toInActive() {
        inputFocused = false
        inputState = MessageCompositionInputState.INACTIVE
    }

    fun toActive(isFocused: Boolean) {
        inputFocused = isFocused
        inputState = MessageCompositionInputState.ACTIVE
    }

    fun toEdit() {
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

    fun toComposing() {
        inputFocused = true
        inputType = MessageCompositionInputType.Composing(
            messageCompositionState = messageCompositionHolder.messageComposition
        )
    }

    fun toggleInputSize() {
        inputSize = if (inputSize == MessageCompositionInputSize.COLLAPSED) {
            MessageCompositionInputSize.EXPANDED
        } else {
            MessageCompositionInputSize.COLLAPSED
        }
    }

    fun cancelReply() {
        messageCompositionHolder.clearReply()
    }

    fun onFocused() {

    }

    fun addMentionToMessage(contact: Contact) {
        messageCompositionHolder.addMention(contact)
    }

}

sealed class MessageCompositionInputType(
    val messageCompositionState: MutableState<MessageComposition>
) {
    @Composable
    open fun inputTextColor(): WireTextFieldColors = wireTextFieldColors(
        backgroundColor = Color.Transparent,
        borderColor = Color.Transparent,
        focusColor = Color.Transparent,
        placeholderColor = colorsScheme().secondaryText
    )

    @Composable
    open fun backgroundColor(): Color = colorsScheme().messageComposerBackgroundColor

    class Composing(messageCompositionState: MutableState<MessageComposition>) : MessageCompositionInputType(messageCompositionState) {

        val isSendButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText.isNotBlank()
        }

    }

    class Editing(
        messageCompositionState: MutableState<MessageComposition>,
        val messageCompositionSnapShot: MessageComposition
    ) : MessageCompositionInputType(messageCompositionState) {

        @Composable
        override fun backgroundColor(): Color = colorsScheme().messageComposerEditBackgroundColor

        val isEditButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText != messageCompositionSnapShot.messageText
        }

    }

    class SelfDeleting(
        messageCompositionState: MutableState<MessageComposition>,
        private val onShowEphemeralOptionsMenu: () -> Unit
    ) : MessageCompositionInputType(messageCompositionState) {
        @Composable
        override fun inputTextColor() =
            wireTextFieldColors(
                backgroundColor = Color.Transparent,
                borderColor = Color.Transparent,
                focusColor = Color.Transparent,
                placeholderColor = colorsScheme().primary
            )

        val isSendButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText.isNotBlank()
        }

        fun showSelfDeletingTimeOption() {
            onShowEphemeralOptionsMenu()
        }
    }


}

enum class MessageCompositionInputSize {
    COLLAPSED,
    EXPANDED;
}

enum class MessageCompositionInputState {
    ACTIVE,
    INACTIVE
}
