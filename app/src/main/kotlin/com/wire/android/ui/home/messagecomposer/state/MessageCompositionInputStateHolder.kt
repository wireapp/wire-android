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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.wireTextFieldColors
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import com.wire.kalium.logic.util.isPositiveNotNull

class MessageCompositionInputStateHolder(
    private val messageTextFieldValue: State<TextFieldValue>,
    private val messageType: State<MessageType>,
    inputType: MessageCompositionType = MessageCompositionType.Composing(
        messageTextFieldValue = messageTextFieldValue,
        messageType = messageType
    ),
    inputFocused: Boolean = false,
    inputVisibility: Boolean = true,
    inputSize: MessageCompositionInputSize = MessageCompositionInputSize.COLLAPSED,
    inputState: MessageCompositionInputState = MessageCompositionInputState.INACTIVE
) {
    var inputFocused: Boolean by mutableStateOf(inputFocused)
        private set

    var inputType: MessageCompositionType by mutableStateOf(inputType)

    var inputVisibility by mutableStateOf(inputVisibility)
        private set

    var inputState: MessageCompositionInputState by mutableStateOf(
        inputState
    )

    var inputSize by mutableStateOf(
        inputSize
    )
        private set

    fun toInActive() {
        inputVisibility = true
        inputSize = MessageCompositionInputSize.COLLAPSED
        inputState = MessageCompositionInputState.INACTIVE
        clearFocus()
    }

    fun toActive(isFocused: Boolean) {
        inputVisibility = true
        inputSize = MessageCompositionInputSize.COLLAPSED
        inputState = MessageCompositionInputState.ACTIVE
        if (isFocused) requestFocus() else clearFocus()
    }

    fun toEdit() {
        inputType = MessageCompositionType.Editing(
            messageTextFieldValue = messageTextFieldValue,
            messageTextFieldValueSnapShot = messageTextFieldValue.value
        )
        toActive(true)
    }

    fun toComposing() {
        inputType = MessageCompositionType.Composing(
            messageTextFieldValue = messageTextFieldValue,
            messageType = messageType
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

    fun show() {
        inputVisibility = true
    }

    fun hide() {
        inputVisibility = false
    }
}

sealed class MessageCompositionType {
    @Composable
    open fun inputTextColor(): WireTextFieldColors = wireTextFieldColors(
        backgroundColor = Color.Transparent,
        borderColor = Color.Transparent,
        focusColor = Color.Transparent,
        placeholderColor = colorsScheme().secondaryText
    )

    @Composable
    open fun backgroundColor(): Color = colorsScheme().messageComposerBackgroundColor

    class Composing(messageTextFieldValue: State<TextFieldValue>, val messageType: State<MessageType>) :
        MessageCompositionType() {

        val isSendButtonEnabled by derivedStateOf {
            messageTextFieldValue.value.text.isNotBlank()
        }

        @Composable
        override fun inputTextColor(): WireTextFieldColors = if (messageType.value is MessageType.SelfDeleting) {
            wireTextFieldColors(
                backgroundColor = Color.Transparent,
                borderColor = Color.Transparent,
                focusColor = Color.Transparent,
                placeholderColor = colorsScheme().primary
            )
        } else {
            super.inputTextColor()
        }
    }

    class Editing(
        messageTextFieldValue: State<TextFieldValue>,
        private val messageTextFieldValueSnapShot: TextFieldValue
    ) : MessageCompositionType() {

        @Composable
        override fun backgroundColor(): Color = colorsScheme().messageComposerEditBackgroundColor

        val isEditButtonEnabled by derivedStateOf {
            messageTextFieldValue.value.text != messageTextFieldValueSnapShot.text
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

sealed class MessageType {
    object Normal : MessageType()
    data class SelfDeleting(val selfDeletionTimer: SelfDeletionTimer) : MessageType()
}
