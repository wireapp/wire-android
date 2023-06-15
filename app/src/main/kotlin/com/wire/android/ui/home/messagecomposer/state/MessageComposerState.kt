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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

sealed class MessageComposerState {
    data class InActive(val messageComposition: MessageComposition) : MessageComposerState()
    class Active(
        val messageCompositionState: MutableState<MessageComposition>,
        defaultInputFocused: Boolean = true,
        defaultInputType: MessageCompositionInputType = MessageCompositionInputType.Composing(messageCompositionState),
        defaultInputSize: MessageCompositionInputSize = MessageCompositionInputSize.COLLAPSED,
        defaultAdditionalOptionsSubMenuState: AdditionalOptionSubMenuState = AdditionalOptionSubMenuState.Hidden,
        private val onShowEphemeralOptionsMenu: () -> Unit
    ) : MessageComposerState() {

        val messageCompositionInputState = MessageCompositionInputState(
            messageCompositionState = messageCompositionState,
            inputFocused = defaultInputFocused,
            inputType = defaultInputType,
            inputSize = defaultInputSize
        )

        var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(defaultAdditionalOptionsSubMenuState)
            private set

        fun toEphemeralInputType() {
            messageCompositionInputState.toSelfDeleting(
                onShowEphemeralOptionsMenu
            )
        }

        fun toggleAttachmentOptions() {
            additionalOptionsSubMenuState =
                if (additionalOptionsSubMenuState == AdditionalOptionSubMenuState.AttachFile) {
                    messageCompositionInputState.inputFocus()
                    AdditionalOptionSubMenuState.Hidden
                } else {
                    messageCompositionInputState.clearFocus()
                    AdditionalOptionSubMenuState.AttachFile
                }
        }

        fun onInputFocused() {
            messageCompositionInputState.inputFocus()
            additionalOptionsSubMenuState = AdditionalOptionSubMenuState.Hidden
        }

        fun messageTextChanged(textFieldValue: TextFieldValue) {
            messageCompositionState.update {
                it.copy(textFieldValue = textFieldValue)
            }
        }

    }

    object AudioRecording : MessageComposerState()
}

fun MutableState<MessageComposition>.update(block: (MessageComposition) -> MessageComposition) {
    val currentValue = value
    value = block(currentValue)
}
