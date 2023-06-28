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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration


class MessageCompositionInputStateHolder(
    private val messageComposition: MutableState<MessageComposition>,
    selfDeletionTimer: State<SelfDeletionTimer>,
    initialInputType: MessageCompositionInputType = MessageCompositionInputType.Composing(messageComposition),
    initialInputState: MessageCompositionInputState = MessageCompositionInputState.INACTIVE,
) {
    var inputFocused: Boolean by mutableStateOf(selfDeletionTimer.value.toDuration() > Duration.ZERO)
        private set

    val inputType by derivedStateOf {
        if (selfDeletionTimer.value.toDuration() > Duration.ZERO) {
            MessageCompositionInputType.SelfDeleting(
                messageCompositionState = messageComposition
            )
        } else _inputType
    }

    private var _inputType: MessageCompositionInputType by mutableStateOf(initialInputType)

     var inputState: MessageCompositionInputState by mutableStateOf(initialInputState)

    var inputSize by mutableStateOf(
        MessageCompositionInputSize.COLLAPSED
    )
        private set

    fun toInActive() {
        inputState = MessageCompositionInputState.INACTIVE
        clearFocus()
    }

    fun toActive(isFocused: Boolean) {
        inputState = MessageCompositionInputState.ACTIVE
        if (isFocused) requestFocus() else clearFocus()
    }

    fun toEdit() {
        _inputType = MessageCompositionInputType.Editing(
            messageCompositionState = messageComposition,
            messageCompositionSnapShot = messageComposition.value
        )
        toActive(true)
    }

    fun toSelfDeleting() {
        _inputType = MessageCompositionInputType.SelfDeleting(
            messageCompositionState = messageComposition
        )
        toActive(true)
    }

    fun toComposing() {
        _inputType = MessageCompositionInputType.Composing(
            messageCompositionState = messageComposition
        )
        toActive(true)
    }

    fun toggleInputSize() {
        inputSize = if (inputSize == MessageCompositionInputSize.COLLAPSED) {
            MessageCompositionInputSize.EXPANDED
        } else {
            MessageCompositionInputSize.COLLAPSED
        }
    }

    fun clearFocus() {
        inputFocused = false
    }

    fun requestFocus() {
        inputFocused = true
    }

}

sealed class MessageCompositionInputType {
    @Composable
    open fun inputTextColor(): WireTextFieldColors = wireTextFieldColors(
        backgroundColor = Color.Transparent,
        borderColor = Color.Transparent,
        focusColor = Color.Transparent,
        placeholderColor = colorsScheme().secondaryText
    )

    @Composable
    open fun backgroundColor(): Color = colorsScheme().messageComposerBackgroundColor

    class Composing(messageCompositionState: MutableState<MessageComposition>) : MessageCompositionInputType() {

        val isSendButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText.isNotBlank()
        }

    }

    class Editing(
        messageCompositionState: MutableState<MessageComposition>,
        val messageCompositionSnapShot: MessageComposition
    ) : MessageCompositionInputType() {

        @Composable
        override fun backgroundColor(): Color = colorsScheme().messageComposerEditBackgroundColor

        val isEditButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText != messageCompositionSnapShot.messageText
        }

    }

    class SelfDeleting(
        messageCompositionState: MutableState<MessageComposition>,
    ) : MessageCompositionInputType() {
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
