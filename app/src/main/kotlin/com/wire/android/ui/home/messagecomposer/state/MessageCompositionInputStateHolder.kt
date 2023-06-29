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
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import kotlin.time.Duration

class MessageCompositionInputStateHolder(
    private val messageComposition: MutableState<MessageComposition>,
    selfDeletionTimer: State<SelfDeletionTimer>,
) {
    var inputFocused: Boolean by mutableStateOf(false)
        private set

    private val messageType = derivedStateOf {
        if (selfDeletionTimer.value is SelfDeletionTimer.Enabled) {
            MessageType.SelfDeleting(selfDeletionTimer.value)
        } else {
            _messageType
        }
    }
    private var _messageType: MessageType by mutableStateOf(MessageType.Normal)

    var inputType: MessageCompositionType by mutableStateOf(
        MessageCompositionType.Composing(
            messageCompositionState = messageComposition,
            messageType = messageType
        )
    )

    var inputState: MessageCompositionInputState by mutableStateOf(MessageCompositionInputState.INACTIVE)

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
        inputType = MessageCompositionType.Editing(
            messageCompositionState = messageComposition,
            messageCompositionSnapShot = messageComposition.value
        )
        toActive(true)
    }

    fun toSelfDeleting() {
        _messageType = if (messageType.value is MessageType.SelfDeleting) {
            messageType.value
        } else {
            MessageType.SelfDeleting(SelfDeletionTimer.Enabled(Duration.ZERO))
        }

        inputType = MessageCompositionType.Composing(
            messageCompositionState = messageComposition,
            messageType = messageType
        )
        toActive(true)
    }

    fun toComposing() {
        inputType = MessageCompositionType.Composing(
            messageCompositionState = messageComposition,
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

    class Composing(messageCompositionState: MutableState<MessageComposition>, val messageType: State<MessageType>) :
        MessageCompositionType() {

        val isSendButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText.isNotBlank()
        }
    }

    class Editing(
        messageCompositionState: MutableState<MessageComposition>,
        val messageCompositionSnapShot: MessageComposition
    ) : MessageCompositionType() {

        @Composable
        override fun backgroundColor(): Color = colorsScheme().messageComposerEditBackgroundColor

        val isEditButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText != messageCompositionSnapShot.messageText
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
