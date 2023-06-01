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

    class Active(
        val messageComposition: MutableState<MessageComposition>,
        additionalOptionMenuState: AdditionalOptionMenuState,
        additionalOptionsSubMenuState: AdditionalOptionSubMenuState,
        messageCompositionInputType: MessageCompositionInputType.Composing,
        messageCompositionInputSize: MessageCompositionInputSize,
    ) : MessageComposerState() {

        var messageCompositionInputType: MessageCompositionInputType by mutableStateOf(messageCompositionInputType)

        var inputSize: MessageCompositionInputSize by mutableStateOf(messageCompositionInputSize)

        var additionalOptionMenuState: AdditionalOptionMenuState by mutableStateOf(additionalOptionMenuState)

        var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(additionalOptionsSubMenuState)

        fun toEphemeralInputType() {
            messageCompositionInputType = MessageCompositionInputType.Ephemeral
        }

        fun messageTextChanged(textFieldValue: TextFieldValue) {
            messageComposition.update { copy(textFieldValue = textFieldValue) }
        }

        fun toggleAttachmentOptions() {
            additionalOptionsSubMenuState =
                if (additionalOptionsSubMenuState == AdditionalOptionSubMenuState.AttachFile) {
                    AdditionalOptionSubMenuState.None
                } else {
                    AdditionalOptionSubMenuState.AttachFile
                }
        }


        fun toggleGifMenu() {

        }

        private fun MutableState<MessageComposition>.update(function: MessageComposition.() -> MessageComposition) {
            value = value.function()
        }
    }

    data class InActive(
        val messageComposition: MessageComposition
    ) : MessageComposerState()

}
